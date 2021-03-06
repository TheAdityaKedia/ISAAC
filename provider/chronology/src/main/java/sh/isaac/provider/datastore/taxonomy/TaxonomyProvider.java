/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * You may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributions from 2013-2017 where performed either by US government 
 * employees, or under US Veterans Health Administration contracts. 
 *
 * US Veterans Health Administration contributions by government employees
 * are work of the U.S. Government and are not subject to copyright
 * protection in the United States. Portions contributed by government 
 * employees are USGovWork (17USC §105). Not subject to copyright. 
 * 
 * Contribution by contractors to the US Veterans Health Administration
 * during this period are contractually contributed under the
 * Apache License, Version 2.0.
 *
 * See: https://www.usa.gov/government-works
 * 
 * Contributions prior to 2013:
 *
 * Copyright (C) International Health Terminology Standards Development Organisation.
 * Licensed under the Apache License, Version 2.0.
 *
 */
package sh.isaac.provider.datastore.taxonomy;

//~--- JDK imports ------------------------------------------------------------
import sh.isaac.model.taxonomy.TaxonomyRecordPrimitive;
import java.lang.ref.WeakReference;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

//~--- non-JDK imports --------------------------------------------------------
import javafx.application.Platform;

import javafx.beans.value.ObservableValue;

import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;

//~--- JDK imports ------------------------------------------------------------
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

//~--- non-JDK imports --------------------------------------------------------
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.glassfish.hk2.runlevel.RunLevel;

import org.jvnet.hk2.annotations.Service;

import sh.isaac.api.ConceptActiveService;
import sh.isaac.api.Get;
import sh.isaac.api.IdentifierService;
import sh.isaac.api.LookupService;
import sh.isaac.api.RefreshListener;
import sh.isaac.api.Status;
import sh.isaac.api.SystemStatusService;
import sh.isaac.api.TaxonomyLink;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.collections.IntSet;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.commit.ChronologyChangeListener;
import sh.isaac.api.commit.CommitRecord;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.coordinate.PremiseType;
import sh.isaac.api.coordinate.StampCoordinate;
import sh.isaac.api.coordinate.StampPrecedence;
import sh.isaac.api.datastore.DataStore;
import sh.isaac.api.tree.Tree;
import sh.isaac.api.tree.TreeNodeVisitData;
import sh.isaac.model.ModelGet;
import sh.isaac.model.TaxonomyDebugService;
import sh.isaac.model.coordinate.ManifoldCoordinateImpl;
import sh.isaac.model.coordinate.StampCoordinateImpl;
import sh.isaac.model.coordinate.StampPositionImpl;
import sh.isaac.provider.datastore.chronology.ChronologyUpdate;
import sh.isaac.api.TaxonomySnapshot;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.tree.TaxonomyLinkage;

//~--- classes ----------------------------------------------------------------
/**
 *
 * @author kec
 */
@Service
@RunLevel(value = LookupService.SL_L4)
public class TaxonomyProvider
        implements TaxonomyDebugService, ConceptActiveService, ChronologyChangeListener {

    /**
     * The Constant LOG.
     */
    private static final Logger LOG = LogManager.getLogger();
    private static final int MAX_AVAILABLE = Runtime.getRuntime()
            .availableProcessors() * 2;

    //~--- fields --------------------------------------------------------------
    private final Semaphore updatePermits = new Semaphore(MAX_AVAILABLE);

    /**
     * The semantic nids for unhandled changes.
     */
    private final ConcurrentSkipListSet<Integer> semanticNidsForUnhandledChanges = new ConcurrentSkipListSet<>();

    private final Set<Task<?>> pendingUpdateTasks = ConcurrentHashMap.newKeySet();
    /**
     * The tree cache.
     */
    private final ConcurrentHashMap<SnapshotCacheKey, Task<Tree>> snapshotCache = new ConcurrentHashMap<>(5);
    private final ConcurrentHashMap<SnapshotCacheKey, TaxonomySnapshot> noTreeSnapshotCache = new ConcurrentHashMap<>(5);
    private final UUID listenerUUID = UUID.randomUUID();

    /**
     * The change listeners.
     */
    ConcurrentSkipListSet<WeakReference<RefreshListener>> refreshListeners = new ConcurrentSkipListSet<>();

    /**
     * The identifier service.
     */
    private IdentifierService identifierService;
    private DataStore store;

    //~--- constructors --------------------------------------------------------
    public TaxonomyProvider() {
    }

    //~--- methods -------------------------------------------------------------
    @Override
    public void addTaxonomyRefreshListener(RefreshListener refreshListener) {
        refreshListeners.add(new WeakReferenceRefreshListener(refreshListener));
    }

    @Override
    public String describeTaxonomyRecord(int nid) {
        return getTaxonomyRecord(nid).toString();
    }

    public Set<Task<?>> getPendingUpdateTasks() {
        return pendingUpdateTasks;
    }

    @Override
    public void handleChange(ConceptChronology cc) {
        // not processing concept changes
        // is this call redundant/better than updateStatus(ConceptChronology conceptChronology) call/method?
    }

    @Override
    public void handleChange(SemanticChronology sc) {
        if (sc.getVersionType() == VersionType.LOGIC_GRAPH) {
            this.semanticNidsForUnhandledChanges.add(sc.getNid());
        }
    }

    @Override
    public void handleCommit(CommitRecord commitRecord) {
        // If a logic graph changed, clear our cache.
        if (this.semanticNidsForUnhandledChanges.size() > 0) {
            LOG.debug("Clearing snapshot cache due to commit");
            this.snapshotCache.clear();
            this.noTreeSnapshotCache.clear();
        }

        this.updatePermits.acquireUninterruptibly();
        UpdateTaxonomyAfterCommitTask updateTask
                = UpdateTaxonomyAfterCommitTask.get(this, commitRecord, this.semanticNidsForUnhandledChanges, this.updatePermits);
        try {
            //wait for completion
            updateTask.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unexpected error waiting for taxonomy update after commit", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void notifyTaxonomyListenersToRefresh() {
        LOG.debug("Clearing snapshot cache due notify request");
        snapshotCache.clear();
        this.noTreeSnapshotCache.clear();
        Platform.runLater(
                () -> {
                    for (WeakReference<RefreshListener> listenerReference : refreshListeners) {
                        RefreshListener listener = listenerReference.get();

                        if (listener != null) {
                            listener.refresh();
                        }
                    }
                });
    }

    @Override
    public Future<?> sync() {
        return Get.executor().submit(() -> {
            for (Task<?> updateTask : pendingUpdateTasks) {
                try {
                    LOG.info("Waiting for completion of: " + updateTask.getTitle());
                    updateTask.get();
                    LOG.info("Completed: " + updateTask.getTitle());
                } catch (Throwable ex) {
                    LOG.error(ex);
                }
            }
            this.store.sync().get();
            return null;
        });
    }

    @Override
    public void updateStatus(ConceptChronology conceptChronology) {
        ChronologyUpdate.handleStatusUpdate(conceptChronology);
    }

    @Override
    public void updateTaxonomy(SemanticChronology logicGraphChronology) {
        LOG.debug("Updating taxonomy for commit to {}", () -> logicGraphChronology.toString());
        try {
            ChronologyUpdate.handleTaxonomyUpdate(logicGraphChronology);
        } catch (Throwable e) {
            LOG.error("error processing taxonomy update", e);
            throw e;
        }
    }

//    @Override
//    public boolean wasEverKindOf(int childId, int parentId) {
//        throw new UnsupportedOperationException(
//                "Not supported yet.");  // To change body of generated methods, choose Tools | Templates.
//    }
    /**
     * Start me.
     */
    @PostConstruct
    private void startMe() {
        try {
            LOG.info("Starting TaxonomyProvider post-construct");
            this.store = Get.service(DataStore.class);
            Get.commitService()
                    .addChangeListener(this);
            this.identifierService = Get.identifierService();
            this.semanticNidsForUnhandledChanges.clear();
            this.pendingUpdateTasks.clear();
            this.snapshotCache.clear();
            this.noTreeSnapshotCache.clear();
            this.refreshListeners.clear();
        } catch (final Exception e) {
            LookupService.getService(SystemStatusService.class)
                    .notifyServiceConfigurationFailure("Taxonomy Provider", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop me.
     */
    @PreDestroy
    private void stopMe() {
        LOG.info("Stopping TaxonomyProvider");
        try {
            // ensure all pending operations have completed. 
            for (Task<?> updateTask : this.pendingUpdateTasks) {
                updateTask.get();
            }
            this.sync().get();
            // make sure updates are done prior to allowing other services to stop.
            this.updatePermits.acquireUninterruptibly(MAX_AVAILABLE);
            this.updatePermits.release(MAX_AVAILABLE);
            this.semanticNidsForUnhandledChanges.clear();
            this.pendingUpdateTasks.clear();
            this.snapshotCache.clear();
            this.noTreeSnapshotCache.clear();
            this.refreshListeners.clear();
            this.identifierService = null;
            this.store = null;
            Get.commitService().removeChangeListener(this);
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Exception during service stop. ", ex);
        }
        LOG.info("BdbTaxonomyProvider stopped");
    }

    //~--- get methods ---------------------------------------------------------
    @Override
    public IntStream getAllRelationshipOriginNidsOfType(int destinationId, IntSet typeSequenceSet) {
        throw new UnsupportedOperationException(
                "Not supported yet.");  // To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isConceptActive(int conceptNid, StampCoordinate stampCoordinate) {
        int assemblageNid = identifierService.getAssemblageNid(conceptNid).getAsInt();
        int[] taxonomyData = store.getTaxonomyData(assemblageNid, conceptNid);

        if (taxonomyData == null) {
            return false;
        }

        TaxonomyRecordPrimitive taxonomyRecord = new TaxonomyRecordPrimitive(taxonomyData);

        try {
            return taxonomyRecord.isConceptActive(conceptNid, stampCoordinate);
        } catch (NoSuchElementException ex) {
            StringBuilder builder = new StringBuilder();
            builder.append("Error determining if concept is active.");
            builder.append(Get.conceptSpecification(conceptNid));
            LOG.error(builder.toString(), ex);
            return false;
        }
    }

    @Override
    public EnumSet<Status> getConceptStates(int conceptNid, StampCoordinate stampCoordinate) {
        int assemblageNid = identifierService.getAssemblageNid(conceptNid).getAsInt();
        int[] taxonomyData = store.getTaxonomyData(assemblageNid, conceptNid);

        if (taxonomyData == null) {
            return EnumSet.noneOf(Status.class);
        }

        TaxonomyRecordPrimitive taxonomyRecord = new TaxonomyRecordPrimitive(taxonomyData);

        return taxonomyRecord.getConceptStates(conceptNid, stampCoordinate);
    }

    @Override
    public Optional<UUID> getDataStoreId() {
        return this.store.getDataStoreId();
    }

    @Override
    public Path getDataStorePath() {
        return this.store.getDataStorePath();
    }

    @Override
    public DataStoreStartState getDataStoreStartState() {
        return this.store.getDataStoreStartState();
    }

    @Override
    public UUID getListenerUuid() {
        return listenerUUID;
    }

    @Override
    public TaxonomySnapshot getStatedLatestSnapshot(int pathNid, Set<ConceptSpecification> modules, EnumSet<Status> allowedStates, boolean computeTree) {
        return computeTree ? 
                getSnapshot(new ManifoldCoordinateImpl(
                    new StampCoordinateImpl(StampPrecedence.TIME,
                        new StampPositionImpl(Long.MAX_VALUE, pathNid),
                        modules, new ArrayList<>(), allowedStates), null)) :
                getSnapshotNoTree(new ManifoldCoordinateImpl(
                        new StampCoordinateImpl(StampPrecedence.TIME,
                                new StampPositionImpl(Long.MAX_VALUE, pathNid),
                                modules, new ArrayList<>(), allowedStates), null));
    }

    @Override
    public TaxonomySnapshot getSnapshot(ManifoldCoordinate tc) {
        Task<Tree> treeTask = getTaxonomyTree(tc);

        return new TaxonomySnapshotProvider(tc, treeTask);
    }
    

    @Override
    public TaxonomySnapshot getSnapshotNoTree(ManifoldCoordinate mc) {
        //The TaxonomySnapshotNoTree does keep a cache of child to parent items, so we cache the entire structure as well, per 
        //manifold coordinate
        return noTreeSnapshotCache.computeIfAbsent(new SnapshotCacheKey(mc), (key) -> new TaxonomySnapshotNoTree(mc));
    }

    private TaxonomyRecordPrimitive getTaxonomyRecord(int nid) {
        int conceptAssemblageNid = ModelGet.identifierService()
                .getAssemblageNid(nid).getAsInt();
        int[] record = store.getTaxonomyData(conceptAssemblageNid, nid);

        return new TaxonomyRecordPrimitive(record);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getTaxonomyData(int assemblageNid, int conceptNid) {
       return store.getTaxonomyData(assemblageNid, conceptNid);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int[] accumulateAndGetTaxonomyData(int assemblageNid, int conceptNid, int[] newData, BinaryOperator<int[]> accumulatorFunction) {
       return store.accumulateAndGetTaxonomyData(assemblageNid, conceptNid, newData, accumulatorFunction);
    }

    private class SnapshotCacheKey {

        PremiseType taxPremiseType;
        StampCoordinate stampCoordinate;

        public SnapshotCacheKey(ManifoldCoordinate tc) {
            this.taxPremiseType = tc.getTaxonomyPremiseType();
            this.stampCoordinate = tc.getStampCoordinate();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.taxPremiseType);
            hash = 29 * hash + this.stampCoordinate.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SnapshotCacheKey other = (SnapshotCacheKey) obj;
            if (this.taxPremiseType != other.taxPremiseType) {
                return false;
            }
            if (!Objects.equals(this.stampCoordinate, other.stampCoordinate)) {
                return false;
            }
            return true;
        }

    }

    public Task<Tree> getTaxonomyTree(ManifoldCoordinate tc) {
        SnapshotCacheKey snapshotCacheKey = new SnapshotCacheKey(tc);
        final Task<Tree> treeTask = this.snapshotCache.get(snapshotCacheKey);

        if (treeTask != null) {
            return treeTask;
        }

        LOG.debug("Building tree for {}, cache key {}", tc, snapshotCacheKey.hashCode());
        IntFunction<int[]> taxonomyDataProvider = new IntFunction<int[]>() {
            final int assemblageNid = tc.getLogicCoordinate().getConceptAssemblageNid();
            @Override
            public int[] apply(int conceptNid) {
                return store.getTaxonomyData(assemblageNid, conceptNid);
            }
        };
        
        TreeBuilderTask treeBuilderTask = new TreeBuilderTask(taxonomyDataProvider, tc);

        Task<Tree> previousTask = this.snapshotCache.putIfAbsent(snapshotCacheKey, treeBuilderTask);

        if (previousTask != null) {
            Get.activeTasks().remove(treeBuilderTask);
            return previousTask;
        }

        Get.executor().execute(treeBuilderTask);

        return treeBuilderTask;
    }

    @Override
    public Supplier<TreeNodeVisitData> getTreeNodeVisitDataSupplier(int conceptAssemblageNid) {
        return () -> new TreeNodeVisitDataImpl(conceptAssemblageNid);
    }

    //~--- inner classes -------------------------------------------------------
    /**
     * The Class TaxonomySnapshotProvider.
     */
    private class TaxonomySnapshotProvider
            implements TaxonomySnapshot {

        int isaNid = TermAux.IS_A.getNid();
        int childOfNid = TermAux.CHILD_OF.getNid();
        NidSet childOfTypeNidSet = new NidSet();
        NidSet isaTypeNidSet = new NidSet();

        /**
         * The manifoldCoordinate.
         */
        final ManifoldCoordinate manifoldCoordinate;
        Tree treeSnapshot;
        final Task<Tree> treeTask;

        //~--- initializers -----------------------------------------------------
        {
            isaTypeNidSet.add(isaNid);
            childOfTypeNidSet.add(childOfNid);
        }

        //~--- constructors -----------------------------------------------------
        public TaxonomySnapshotProvider(ManifoldCoordinate manifoldCoordinate, Task<Tree> treeTask) {
            this.manifoldCoordinate = manifoldCoordinate;
            this.treeTask = treeTask;

            if (!treeTask.isDone()) {
                if (Platform.isFxApplicationThread()) {
                    this.treeTask.stateProperty()
                            .addListener(this::succeeded);
                } else {
                    Platform.runLater(
                            () -> {
                                Task<Tree> theTask = treeTask;

                                if (!theTask.isDone()) {
                                    theTask.stateProperty()
                                            .addListener(this::succeeded);
                                } else {
                                    try {
                                        this.treeSnapshot = treeTask.get();
                                    } catch (InterruptedException | ExecutionException ex) {
                                        LOG.error("Unexpected error constructing taxonomy snapshot provider", ex);
                                    }
                                }

                            });
                }
            }

            if (treeTask.isDone()) {
                try {
                    this.treeSnapshot = treeTask.get();
                } catch (InterruptedException | ExecutionException ex) {
                    LOG.error("Unexpected error constructing taxonomy snapshot provider", ex);
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override
        public TaxonomySnapshot makeAnalog(ManifoldCoordinate manifoldCoordinate) {
            return TaxonomyProvider.this.getSnapshot(manifoldCoordinate);
        }

        @Override
        public Collection<TaxonomyLink> getTaxonomyParentLinks(int parentConceptNid) {
            int[] parentNids = getTaxonomyParentConceptNids(parentConceptNid);
            ArrayList<TaxonomyLink> links = new ArrayList(parentNids.length);
            for (int parentNid: parentNids) {
                links.add(new TaxonomyLinkage(TermAux.IS_A.getNid(), parentNid));
            }
            return links;
        }

        @Override
        public Collection<TaxonomyLink> getTaxonomyChildLinks(int childConceptNid) {
            int[] childNids = getTaxonomyChildConceptNids(childConceptNid);
            ArrayList<TaxonomyLink> links = new ArrayList(childNids.length);
            for (int childNid: childNids) {
                links.add(new TaxonomyLinkage(TermAux.IS_A.getNid(), childNid));
            }
            return links;
        }

        private void succeeded(ObservableValue<? extends State> observable, State oldValue, State newValue) {
            try {
                switch (newValue) {
                    case SUCCEEDED: {
                        this.treeSnapshot = treeTask.get();
                    }
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOG.error("Unexpected error in succeeded call", ex);
                throw new RuntimeException(ex);
            }
        }

        //~--- get methods ------------------------------------------------------
        /**
         * Checks if child of.
         *
         * @param childId the child id
         * @param parentId the parent id
         * @return true, if child of
         */
        @Override
        public boolean isChildOf(int childId, int parentId) {
            if (treeSnapshot != null) {
                return this.treeSnapshot.isChildOf(childId, parentId);
            }

            TaxonomyRecordPrimitive taxonomyRecordPrimitive = getTaxonomyRecord(childId);

            return taxonomyRecordPrimitive.containsNidViaType(parentId, isaNid, manifoldCoordinate);
        }

        /**
         * Checks if kind of.
         *
         * @param childId the child id
         * @param kindofNid the parent id
         * @return true, if kind of
         */
        @Override
        public boolean isKindOf(int childId, int kindofNid) {
            if (treeSnapshot != null) {
                return this.treeSnapshot.isDescendentOf(childId, kindofNid);
            }

            if (isChildOf(childId, kindofNid)) {
                return true;
            }

            for (int parentNid : getTaxonomyParentConceptNids(childId)) {
                if (isKindOf(parentNid, kindofNid, 0)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isKindOf(int childId, int kindofNid, int depth) {
            if (depth > 40) {
                LOG.warn("Taxonomy depth > 40: " + depth + "; \n" + Get.conceptDescriptionText(childId) + " <? \n" + Get.conceptDescriptionText(kindofNid));
            }
            if (depth > 60) {
                LOG.error("Taxonomy depth > 60" + Get.conceptDescriptionText(childId) + " <? " + Get.conceptDescriptionText(kindofNid));
                LOG.error("Return false secondary to presumed cycle. ");
                // TODO raise alert to user via alert mechanism. 
                return false;
            }
            if (isChildOf(childId, kindofNid)) {
                return true;
            }

            for (int parentNid : getTaxonomyParentConceptNids(childId)) {
                if (isKindOf(parentNid, kindofNid, depth + 1)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Gets the kind of sequence set.
         *
         * @param rootId the root id
         * @return the kind of sequence set
         */
        @Override
        public NidSet getKindOfConceptNidSet(int rootId) {
            if (treeSnapshot != null) {
                NidSet kindOfSet = this.treeSnapshot.getDescendentNidSet(rootId);

                kindOfSet.add(rootId);
                return kindOfSet;
            }

            int[] childNids = getTaxonomyChildConceptNids(rootId);
            NidSet kindOfSet = NidSet.of(getTaxonomyChildConceptNids(rootId));

            for (int childNid : childNids) {
                kindOfSet.addAll(getKindOfConceptNidSet(childNid));
            }

            return kindOfSet;
        }

        @Override
        public ManifoldCoordinate getManifoldCoordinate() {
            return this.manifoldCoordinate;
        }

        /**
         * Gets the roots.
         *
         * @return the roots
         */
        @Override
        public int[] getRootNids() {
            if (treeSnapshot != null) {
                return treeSnapshot.getRootNids();
            }

            return new int[]{TermAux.SOLOR_ROOT.getNid()};
        }

        /**
         * Gets the taxonomy child sequences.
         *
         * @param parentId the parent id
         * @return the taxonomy child sequences
         */
        @Override
        public int[] getTaxonomyChildConceptNids(int parentId) {
            if (treeSnapshot != null) {
                return this.treeSnapshot.getTaxonomyChildConceptNids(parentId);
            }

            TaxonomyRecordPrimitive taxonomyRecordPrimitive = getTaxonomyRecord(parentId);

            return taxonomyRecordPrimitive.getDestinationNidsOfType(childOfTypeNidSet, manifoldCoordinate);
        }

        @Override
        public boolean isLeaf(int conceptNid) {
            if (treeSnapshot != null) {
                return this.treeSnapshot.getTaxonomyChildConceptNids(conceptNid).length == 0;
            }
            TaxonomyRecordPrimitive taxonomyRecordPrimitive = getTaxonomyRecord(conceptNid);
            return !taxonomyRecordPrimitive.hasDestinationNidsOfType(childOfTypeNidSet, manifoldCoordinate);
        }

        /**
         * Gets the taxonomy parent sequences.
         *
         * @param childId the child id
         * @return the taxonomy parent sequences
         */
        @Override
        public int[] getTaxonomyParentConceptNids(int childId) {
            if (treeSnapshot != null) {
                return this.treeSnapshot.getTaxonomyParentConceptNids(childId);
            }

            TaxonomyRecordPrimitive taxonomyRecordPrimitive = getTaxonomyRecord(childId);

            return taxonomyRecordPrimitive.getDestinationNidsOfType(isaTypeNidSet, manifoldCoordinate);
        }

        /**
         * Gets the taxonomy tree.
         *
         * @return the taxonomy tree
         */
        @Override
        public Tree getTaxonomyTree() {
            try {
                if (treeSnapshot != null) {
                    return this.treeSnapshot;
                }

                return treeTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOG.error("Unexpected error constructing taxonomy snapshot provider", ex);
                throw new RuntimeException(ex);
            }
        }
    }
    
    //An alternate implementation that doesn't compute a tree in the background....
    //TODO merge the code above with this somehow, maybe so the above code falls back to this code when the tree isn't available, rather than
    // copy and paste inheritance.  But for now, this is a test anyway, trying to overcome performance issues with tree calculation.
    private class TaxonomySnapshotNoTree implements TaxonomySnapshot {
        int isaNid = TermAux.IS_A.getNid();
        int childOfNid = TermAux.CHILD_OF.getNid();
        NidSet childOfTypeNidSet = new NidSet();
        NidSet isaTypeNidSet = new NidSet();
        
        ConcurrentHashMap<String, Boolean> childOfCache = new ConcurrentHashMap<>(25);

        final ManifoldCoordinate tc;
        //init code
        {
            isaTypeNidSet.add(isaNid);
            childOfTypeNidSet.add(childOfNid);
        }

        public TaxonomySnapshotNoTree(ManifoldCoordinate tc) {
            LOG.debug("Building a new non-tree taxonomy snapshot for {}", tc);
            this.tc = tc;
        }

        @Override
        public boolean isChildOf(int childId, int parentId) {
            return childOfCache.computeIfAbsent(childId + ":" + parentId, (key) -> 
            {
                TaxonomyRecordPrimitive taxonomyRecordPrimitive = getTaxonomyRecord(childId);
                return taxonomyRecordPrimitive.containsNidViaType(parentId, isaNid, tc);
            });
        }

        @Override
        public boolean isKindOf(int childId, int kindofNid) {
            if (isChildOf(childId, kindofNid)) {
                return true;
            }

            for (int parentNid : getTaxonomyParentConceptNids(childId)) {
                if (isKindOf(parentNid, kindofNid, 0)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isKindOf(int childId, int kindofNid, int depth) {
            if (depth > 40) {
                LOG.warn("Taxonomy depth > 40: " + depth + "; \n" + Get.conceptDescriptionText(childId) + " <? \n" + Get.conceptDescriptionText(kindofNid));
            }
            if (depth > 60) {
                LOG.error("Taxonomy depth > 60" + Get.conceptDescriptionText(childId) + " <? " + Get.conceptDescriptionText(kindofNid));
                LOG.error("Return false secondary to presumed cycle. ");
                // TODO raise alert to user via alert mechanism. 
                return false;
            }
            if (isChildOf(childId, kindofNid)) {
                return true;
            }

            for (int parentNid : getTaxonomyParentConceptNids(childId)) {
                if (isKindOf(parentNid, kindofNid, depth + 1)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public NidSet getKindOfConceptNidSet(int rootId) {
            int[] childNids = getTaxonomyChildConceptNids(rootId);
            NidSet kindOfSet = NidSet.of(getTaxonomyChildConceptNids(rootId));

            for (int childNid : childNids) {
                kindOfSet.addAll(getKindOfConceptNidSet(childNid));
            }

            return kindOfSet;
        }

        @Override
        public ManifoldCoordinate getManifoldCoordinate() {
            return this.tc;
        }

        @Override
        public int[] getRootNids() {
            return new int[] { TermAux.SOLOR_ROOT.getNid() };
        }

        @Override
        public int[] getTaxonomyChildConceptNids(int parentId) {
            TaxonomyRecordPrimitive taxonomyRecordPrimitive = getTaxonomyRecord(parentId);
            return taxonomyRecordPrimitive.getDestinationNidsOfType(childOfTypeNidSet, tc);
        }

        @Override
        public boolean isLeaf(int conceptNid) {
            TaxonomyRecordPrimitive taxonomyRecordPrimitive = getTaxonomyRecord(conceptNid);
            return !taxonomyRecordPrimitive.hasDestinationNidsOfType(childOfTypeNidSet, tc);
        }

        @Override
        public int[] getTaxonomyParentConceptNids(int childId) {
            TaxonomyRecordPrimitive taxonomyRecordPrimitive = getTaxonomyRecord(childId);
            return taxonomyRecordPrimitive.getDestinationNidsOfType(isaTypeNidSet, tc);
        }

        @Override
        public Tree getTaxonomyTree() {
            throw new UnsupportedOperationException("Need to call getSnapshot(), rather than getSnapshotNoTree()");
        }

        @Override
        public TaxonomySnapshot makeAnalog(ManifoldCoordinate manifoldCoordinate) {
            return TaxonomyProvider.this.getSnapshot(manifoldCoordinate);
        }

        @Override
        public Collection<TaxonomyLink> getTaxonomyParentLinks(int parentConceptNid) {
            int[] parentNids = getTaxonomyParentConceptNids(parentConceptNid);
            ArrayList<TaxonomyLink> links = new ArrayList<>(parentNids.length);
            for (int parentNid: parentNids) {
                links.add(new TaxonomyLinkage(TermAux.IS_A.getNid(), parentNid));
            }
            return links;
        }
    
        @Override
        public Collection<TaxonomyLink> getTaxonomyChildLinks(int childConceptNid) {
            int[] childNids = getTaxonomyChildConceptNids(childConceptNid);
            ArrayList<TaxonomyLink> links = new ArrayList<>(childNids.length);
            for (int childNid: childNids) {
                links.add(new TaxonomyLinkage(TermAux.IS_A.getNid(), childNid));
            }
            return links;
        }
    }
}
