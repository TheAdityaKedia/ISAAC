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



package sh.isaac.provider.commit;

//~--- JDK imports ------------------------------------------------------------

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import java.nio.file.Files;
import java.nio.file.Path;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

//~--- non-JDK imports --------------------------------------------------------

import javafx.collections.ObservableList;

import javafx.concurrent.Task;

//~--- JDK imports ------------------------------------------------------------

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mahout.math.map.OpenIntIntHashMap;

import org.glassfish.hk2.runlevel.RunLevel;

import org.jvnet.hk2.annotations.Service;

import sh.isaac.api.ConfigurationService;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.SystemStatusService;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.Version;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.collections.StampSequenceSet;
import sh.isaac.api.collections.UuidIntMapMap;
import sh.isaac.api.commit.ChangeChecker;
import sh.isaac.api.commit.CheckPhase;
import sh.isaac.api.commit.CheckResult;
import sh.isaac.api.commit.ChronologyChangeListener;
import sh.isaac.api.commit.CommitRecord;
import sh.isaac.api.commit.CommitService;
import sh.isaac.api.commit.UncommittedStamp;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.api.externalizable.IsaacExternalizable;
import sh.isaac.api.externalizable.IsaacObjectType;
import sh.isaac.api.externalizable.StampAlias;
import sh.isaac.api.externalizable.StampComment;
import sh.isaac.api.task.SequentialAggregateTask;
import sh.isaac.api.task.TimedTask;
import sh.isaac.model.VersionImpl;
import sh.isaac.model.observable.ObservableChronologyImpl;
import sh.isaac.model.observable.version.ObservableVersionImpl;

//~--- classes ----------------------------------------------------------------

/**
 * The Class CommitProvider.
 *
 * @author kec
 */
@Service(name = "Commit Provider")
@RunLevel(value = 1)
public class CommitProvider
         implements CommitService {
   /**
    * The Constant LOG.
    */
   private static final Logger LOG = LogManager.getLogger();

   /**
    * The Constant DEFAULT_COMMIT_MANAGER_FOLDER.
    */
   public static final String DEFAULT_COMMIT_MANAGER_FOLDER = "commit-manager";

   /**
    * The Constant COMMIT_MANAGER_DATA_FILENAME.
    */
   private static final String COMMIT_MANAGER_DATA_FILENAME = "commit-manager.data";

   /**
    * The Constant STAMP_ALIAS_MAP_FILENAME.
    */
   private static final String STAMP_ALIAS_MAP_FILENAME = "stamp-alias.map";

   /**
    * The Constant STAMP_COMMENT_MAP_FILENAME.
    */
   private static final String STAMP_COMMENT_MAP_FILENAME = "stamp-comment.map";

   /**
    * The Constant WRITE_POOL_SIZE.
    */
   private static final int WRITE_POOL_SIZE = 40;

   //~--- fields --------------------------------------------------------------

   // TODO persist dataStoreId.
   private final UUID dataStoreId = UUID.randomUUID();

   /**
    * The uncommitted nid lock.
    */
   private final ReentrantLock uncommittedSequenceLock = new ReentrantLock();

   /**
    * The write permit reference.
    */
   private final AtomicReference<Semaphore> writePermitReference = new AtomicReference<>(
                                                                       new Semaphore(WRITE_POOL_SIZE));

   /**
    * The write completion service.
    */
   private final WriteCompletionService writeCompletionService = new WriteCompletionService();

   /**
    * The change listeners.
    */
   ConcurrentSkipListSet<WeakReference<ChronologyChangeListener>> changeListeners = new ConcurrentSkipListSet<>();

   /**
    * The checkers.
    */
   private final ConcurrentSkipListSet<ChangeChecker> checkers = new ConcurrentSkipListSet<>();

   /**
    * The last commit.
    */
   private long lastCommit = Long.MIN_VALUE;

   /**
    * The load required.
    */
   private final AtomicBoolean loadRequired = new AtomicBoolean();

   /**
    * The deferred import no check nids.
    */
   AtomicReference<Set<Integer>> deferredImportNoCheckNids = new AtomicReference<>(new ConcurrentSkipListSet<>());

   /**
    * Persistent map of a stamp aliases to a nid.
    */
   private final StampAliasMap stampAliasMap = new StampAliasMap();

   /**
    * Persistent map of comments to a stamp.
    */
   private final StampCommentMap stampCommentMap = new StampCommentMap();

   /**
    * Persistent nid of database commit actions.
    */
   private final AtomicLong databaseSequence = new AtomicLong();

   /**
    * Persistent stamp nid.
    */
   private final NidSet uncommittedConceptsWithChecksNidSet = NidSet.concurrent();

   /**
    * The uncommitted concepts no checks nid set.
    */
   private final NidSet uncommittedConceptsNoChecksNidSet = NidSet.concurrent();

   /**
    * The uncommitted semantics with checks nid set.
    */
   private final NidSet uncommittedSemanticsWithChecksNidSet = NidSet.concurrent();

   /**
    * The uncommitted semantics no checks nid set.
    */
   private final NidSet uncommittedSemanticsNoChecksNidSet = NidSet.concurrent();
   
   private Set<Task<?>> pendingCommitTasks = ConcurrentHashMap.newKeySet();


   /**
    * The database validity.
    */
   private DatabaseValidity databaseValidity = DatabaseValidity.NOT_SET;

   /**
    * The db folder path.
    */
   private final Path dbFolderPath;

   /**
    * The commit manager folder.
    */
   private final Path commitManagerFolder;

   //~--- constructors --------------------------------------------------------

   /**
    * Instantiates a new commit provider.
    *
    * @throws IOException Signals that an I/O exception has occurred.
    */
   private CommitProvider()
            throws IOException {
      try {
         ConfigurationService configurationService = LookupService.getService(ConfigurationService.class);
         Optional<Path>       dataStorePath        = configurationService.getDataStoreFolderPath();

         if (!dataStorePath.isPresent()) {
            throw new IllegalStateException("dataStorePath is not set");
         }

         this.dbFolderPath = dataStorePath.get()
                                          .resolve("commit-provider");
         this.loadRequired.set(Files.exists(this.dbFolderPath));
         Files.createDirectories(this.dbFolderPath);
         this.commitManagerFolder = this.dbFolderPath.resolve(DEFAULT_COMMIT_MANAGER_FOLDER);

         if (!Files.exists(this.commitManagerFolder)) {
            this.databaseValidity = DatabaseValidity.MISSING_DIRECTORY;
         }

         Files.createDirectories(this.commitManagerFolder);
      } catch (final IOException e) {
         LookupService.getService(SystemStatusService.class)
                      .notifyServiceConfigurationFailure("Cradle Commit Provider", e);
         throw e;
      }
   }

   //~--- methods -------------------------------------------------------------

   public Set<Task<?>> getPendingCommitTasks() {
        return pendingCommitTasks;
   }

    /**
     * Adds the alias.
     *
     * @param stampSequence the stamp nid
     * @param stampAlias the stamp alias
     * @param aliasCommitComment the alias commit comment
     */
    @Override
    public void addAlias(int stampSequence, int stampAlias, String aliasCommitComment) {
        this.stampAliasMap.addAlias(stampSequence, stampAlias);
        
        if (aliasCommitComment != null) {
            this.stampCommentMap.addComment(stampAlias, aliasCommitComment);
        }
    }

   /**
    * Adds the change checker.
    *
    * @param checker the checker
    */
   @Override
   public void addChangeChecker(ChangeChecker checker) {
      this.checkers.add(checker);
   }

   /**
    * Due to the use of Weak References in the implementation, you MUST maintain a reference to the change listener that
    * is passed in here, otherwise, it will be rapidly garbage collected, and you will randomly stop getting change
    * notifications!.
    *
    * @param changeListener the change listener
    * @see sh.isaac.api.commit.CommitService#addChangeListener(sh.isaac.api.commit.ChronologyChangeListener)
    */
   @Override
   public void addChangeListener(ChronologyChangeListener changeListener) {
      this.changeListeners.add(new ChangeListenerReference(changeListener));
   }

   /**
    * Adds the uncommitted.
    *
    * @param cc the cc
    * @return the task
    */
   @Override
   public Task<Void> addUncommitted(ConceptChronology cc) {
      if (cc instanceof ObservableChronologyImpl) {
         cc = (ConceptChronology) ((ObservableChronologyImpl) cc).getWrappedChronology();
      }

      return checkAndWrite(cc, this.checkers, this.writePermitReference.get(), this.changeListeners);
   }

   /**
    * Adds the uncommitted.
    *
    * @param sc the sc
    * @return the task
    */
   @Override
   public Task<Void> addUncommitted(SemanticChronology sc) {
      if (sc instanceof ObservableChronologyImpl) {
         sc = (SemanticChronology) ((ObservableChronologyImpl) sc).getWrappedChronology();
      }

      return checkAndWrite(sc, this.checkers, this.writePermitReference.get(), this.changeListeners);
   }

   /**
    * Adds the uncommitted no checks.
    *
    * @param cc the cc
    * @return the task
    */
   @Override
   public Task<Void> addUncommittedNoChecks(ConceptChronology cc) {
      if (cc instanceof ObservableChronologyImpl) {
         cc = (ConceptChronology) ((ObservableChronologyImpl) cc).getWrappedChronology();
      }

      return write(cc, this.writePermitReference.get(), this.changeListeners);
   }

   /**
    * Adds the uncommitted no checks.
    *
    * @param sc the sc
    * @return the task
    */
   @Override
   public Task<Void> addUncommittedNoChecks(SemanticChronology sc) {
      if (sc instanceof ObservableChronologyImpl) {
         sc = (SemanticChronology) ((ObservableChronologyImpl) sc).getWrappedChronology();
      }

      return write(sc, this.writePermitReference.get(), this.changeListeners);
   }

   /**
    * Cancel.
    *
    * @param editCoordinate the edit coordinate
    * @return the task
    */
   @Override
   public Task<Void> cancel(EditCoordinate editCoordinate) {
      return Get.stampService()
                .cancel(editCoordinate.getAuthorNid());
   }

   /**
    * Cancel.
    *
    * @param chronicle the chronicle
    * @param editCoordinate the edit coordinate
    * @return the task
    */
   @Override
   public Task<Void> cancel(Chronology chronicle, EditCoordinate editCoordinate) {
      if (chronicle instanceof ObservableChronologyImpl) {
         chronicle = ((ObservableChronologyImpl) chronicle).getWrappedChronology();
      }

      final List<Version> versionList = chronicle.getVersionList();

      for (final Version version: versionList) {
         if (version.isUncommitted()) {
            if (version.getAuthorNid() == editCoordinate.getAuthorNid()) {
               if (version instanceof VersionImpl) {
                  ((VersionImpl) version).cancel();
               } else if (version instanceof ObservableVersionImpl) {
                  ((ObservableVersionImpl) version).cancel();
               }
            }
         }
      }

      final Collection<Task<?>> subTasks = new ArrayList<>();

      if (chronicle instanceof ConceptChronology) {
         final ConceptChronology conceptChronology = (ConceptChronology) chronicle;

         if (this.uncommittedConceptsNoChecksNidSet.contains(conceptChronology.getNid())) {
            subTasks.add(addUncommittedNoChecks(conceptChronology));
         }

         if (this.uncommittedConceptsWithChecksNidSet.contains(conceptChronology.getNid())) {
            subTasks.add(addUncommitted(conceptChronology));
         }
      } else if (chronicle instanceof SemanticChronology) {
         final SemanticChronology semanticChronology = (SemanticChronology) chronicle;

         if (this.uncommittedSemanticsNoChecksNidSet.contains(semanticChronology.getNid())) {
            subTasks.add(addUncommittedNoChecks(semanticChronology));
         }

         if (this.uncommittedSemanticsWithChecksNidSet.contains(semanticChronology.getNid())) {
            subTasks.add(addUncommitted(semanticChronology));
         }
      } else {
         throw new RuntimeException("Unsupported chronology type: " + chronicle);
      }

      return new SequentialAggregateTask<>("Canceling change", subTasks);
   }

   /**
    * Commit.
    *
    * @param editCoordinate the edit coordinate
    * @param commitComment the commit comment
    * @return the task
    */
   @Override
   public Task<Optional<CommitRecord>> commit(EditCoordinate editCoordinate, String commitComment) {
      // TODO, make this only commit those components with changes from the provided edit coordinate.
      // Also do we need to lock around the CommitTask creation to makre sure the uncommitted lists are consistent during copy?
      Semaphore pendingWrites = writePermitReference.getAndSet(new Semaphore(WRITE_POOL_SIZE));

      pendingWrites.acquireUninterruptibly(WRITE_POOL_SIZE);

      try {
         this.uncommittedSequenceLock.lock();
         lastCommit = databaseSequence.incrementAndGet();

         final Map<UncommittedStamp, Integer> pendingStampsForCommit = Get.stampService()
                                                                          .getPendingStampsForCommit();
         final Map<UncommittedStamp, Integer> stampsToReturn         = new HashMap<>();

         pendingStampsForCommit.forEach(
             (uncommittedStamp, stampSequence) -> {
                if (uncommittedStamp.authorNid != editCoordinate.getAuthorNid()) {
                   stampsToReturn.put(uncommittedStamp, stampSequence);
                }
             });
         Get.stampService()
            .addPendingStampsForCommit(stampsToReturn);

         CommitTask task = CommitTask.get(
                               commitComment,
                               uncommittedConceptsWithChecksNidSet,
                               uncommittedConceptsNoChecksNidSet,
                               uncommittedSemanticsWithChecksNidSet,
                               uncommittedSemanticsNoChecksNidSet,
                               lastCommit,
                               checkers,
                               pendingStampsForCommit,
                               this);

         return task;
      } finally {
         this.uncommittedSequenceLock.unlock();
      }
   }

   /**
    * Commit.
    *
    * @param chronicle the chronicle
    * @param editCoordinate the edit coordinate
    * @param commitComment the commit comment
    * @return the task
    */
   @Override
   public synchronized Task<Optional<CommitRecord>> commit(Chronology chronicle,
         EditCoordinate editCoordinate,
         String commitComment) {
      // TODO make asynchronous with a actual task.
      // TODO there are numerous inconsistencies with this impl, and the global commit. Need to understand:
      // global seq number, should a write be done on the provider?
      // This also doesn't safely copy the uncommitted lists before using them.
      // TODO I think this needs to be rewritten to use the CommitTask - but need to understand these issues first.
      // This method doesn't update the observable provider properly...
      CommitRecord commitRecord = null;

      if (chronicle instanceof ObservableChronologyImpl) {
         chronicle = ((ObservableChronologyImpl) chronicle).getWrappedChronology();
      }

      final AtomicInteger failCount = new AtomicInteger(0);

      if (chronicle instanceof ConceptChronology) {
         final ConceptChronology conceptChronology = (ConceptChronology) chronicle;

         if (this.uncommittedConceptsWithChecksNidSet.contains(conceptChronology.getNid())) {
            this.checkers.stream()
                         .forEach(
                             (check) -> {
                                if (check.check(conceptChronology, CheckPhase.COMMIT) == CheckResult.FAIL) {
                                   failCount.incrementAndGet();
                                }
                             });
         }
      } else if (chronicle instanceof SemanticChronology) {
         final SemanticChronology semanticChronology = (SemanticChronology) chronicle;

         if (this.uncommittedSemanticsWithChecksNidSet.contains(semanticChronology.getNid())) {
            this.checkers.stream()
                         .forEach(
                             (check) -> {
                                if (check.check(semanticChronology, CheckPhase.COMMIT) == CheckResult.FAIL) {
                                   failCount.incrementAndGet();
                                }
                             });
         }
      } else {
         throw new RuntimeException("Unsupported chronology type: " + chronicle);
      }

      if (failCount.get() == 0) {
         final long commitTime = System.currentTimeMillis();

         // TODO have it only commit the versions on the sememe consistent with the edit coordinate.
         // successful check, commit and remove uncommitted sequences...
         final StampSequenceSet  stampsInCommit   = new StampSequenceSet();
         final OpenIntIntHashMap stampAliases     = new OpenIntIntHashMap();
         final NidSet            conceptsInCommit = new NidSet();
         final NidSet            sememesInCommit  = new NidSet();

         chronicle.getVersionList()
                  .forEach(
                      (version) -> {
                         if (version.isUncommitted() && (version.getAuthorNid() == editCoordinate.getAuthorNid())) {
                            version.setTime(commitTime);
                            stampsInCommit.add(version.getStampSequence());
                         }
                      });

         if (chronicle instanceof ConceptChronology) {
            final ConceptChronology conceptChronology = (ConceptChronology) chronicle;

            conceptsInCommit.add(conceptChronology.getNid());
            this.uncommittedConceptsWithChecksNidSet.remove(conceptChronology.getNid());
            this.uncommittedConceptsNoChecksNidSet.remove(conceptChronology.getNid());
            Get.conceptService()
               .writeConcept(conceptChronology);
         } else {
            final SemanticChronology semanticChronology = (SemanticChronology) chronicle;

            sememesInCommit.add(semanticChronology.getNid());
            this.uncommittedSemanticsWithChecksNidSet.remove(semanticChronology.getNid());
            this.uncommittedSemanticsNoChecksNidSet.remove(semanticChronology.getNid());
            Get.assemblageService()
               .writeSemanticChronology(semanticChronology);
         }

         commitRecord = new CommitRecord(
             Instant.ofEpochMilli(commitTime),
             stampsInCommit,
             stampAliases,
             conceptsInCommit,
             sememesInCommit,
             commitComment);
      }

      CommitProvider.this.handleCommitNotification(commitRecord);

      final Optional<CommitRecord>       optionalRecord = Optional.ofNullable(commitRecord);
      final Task<Optional<CommitRecord>> task           = new TimedTask() {
         @Override
         protected Optional<CommitRecord> call()
                  throws Exception {
            Get.activeTasks()
               .remove(this);
            return optionalRecord;
         }
      };

      Get.activeTasks()
         .add(task);
      Get.workExecutors()
         .getExecutor()
         .execute(task);
      return task;
   }

   /**
    * Import no checks.
    *
    * @param isaacExternalizable the ochre externalizable
    */
   @Override
   public void importNoChecks(IsaacExternalizable isaacExternalizable) {
      switch (isaacExternalizable.getIsaacObjectType()) {
      case CONCEPT:
         final ConceptChronology conceptChronology = (ConceptChronology) isaacExternalizable;

         Get.conceptService()
            .writeConcept(conceptChronology);
         break;

      case SEMANTIC:
         final SemanticChronology semanticChronology = (SemanticChronology) isaacExternalizable;

         Get.assemblageService()
            .writeSemanticChronology(semanticChronology);

         if (semanticChronology.getVersionType() == VersionType.LOGIC_GRAPH) {
            deferNidAction(semanticChronology.getNid());
         }

         break;

      case STAMP_ALIAS:
         final StampAlias stampAlias = (StampAlias) isaacExternalizable;

         this.stampAliasMap.addAlias(stampAlias.getStampSequence(), stampAlias.getStampAlias());
         break;

      case STAMP_COMMENT:
         final StampComment stampComment = (StampComment) isaacExternalizable;

         this.stampCommentMap.addComment(stampComment.getStampSequence(), stampComment.getComment());
         break;

      default:
         throw new UnsupportedOperationException(
             "ap Can't handle: " + isaacExternalizable.getClass().getName() + ": " + isaacExternalizable);
      }
   }

   /**
    * Increment and get nid.
    *
    * @return the long
    */
   @Override
   public long incrementAndGetSequence() {
      return this.databaseSequence.incrementAndGet();
   }

   /**
    * Post process import no checks.
    */
   @Override
   public void postProcessImportNoChecks() {
      final Set<Integer> nids = this.deferredImportNoCheckNids.getAndSet(new ConcurrentSkipListSet<>());

      if (nids != null) {
         LOG.info("Post processing import. Deferred set size: " + nids.size());

         int descriptionAssemblageNid = TermAux.DESCRIPTION_ASSEMBLAGE.getNid();

         // int chroniclePropertiesNid = MetaData.CHRONICLE_PROPERTIES____SOLOR.getNid();
         for (final int nid: nids) {
            if (IsaacObjectType.SEMANTIC == Get.identifierService().getObjectTypeForComponent(nid)) {
               final SemanticChronology sc = Get.assemblageService()
                                                .getSemanticChronology(nid);

               if (sc.getVersionType() == VersionType.LOGIC_GRAPH) {
                  if (sc.getReferencedComponentNid() == descriptionAssemblageNid) {
                     // LOG.info("Found definition for TermAux.DESCRIPTION_ASSEMBLAGE: " + sc);
                  }

                  Get.taxonomyService()
                     .updateTaxonomy(sc);
               } else {
                  throw new UnsupportedOperationException("Unexpected nid in deferred set: " + nid);
               }
            } else {
               throw new UnsupportedOperationException("Unexpected nid in deferred set: " + nid);
            }
         }
      }
   }

   /**
    * Removes the change checker.
    *
    * @param checker the checker
    */
   @Override
   public void removeChangeChecker(ChangeChecker checker) {
      this.checkers.remove(checker);
   }

   /**
    * Removes the change listener.
    *
    * @param changeListener the change listener
    */
   @Override
   public void removeChangeListener(ChronologyChangeListener changeListener) {
      this.changeListeners.remove(new ChangeListenerReference(changeListener));
   }

   @Override
   public Future<?> sync() {
      return Get.executor().submit(() -> {
        for (Task<?> updateTask: pendingCommitTasks) {
            try {
                LOG.info("Waiting for completion of: " + updateTask.getTitle());
                updateTask.get();
                LOG.info("Completed: " + updateTask.getTitle());
            } catch (Throwable ex) {
                LOG.error(ex);
            } 
        }
         writeData();
         return null;
      });
   }

   /**
    * Adds the comment.
    *
    * @param stamp the stamp
    * @param commitComment the commit comment
    */
   protected void addComment(int stamp, String commitComment) {
      this.stampCommentMap.addComment(stamp, commitComment);
   }

   /**
    * Handle commit notification.
    *
    * @param commitRecord the commit record
    */
   protected void handleCommitNotification(CommitRecord commitRecord) {
      this.changeListeners.forEach(
          (listenerRef) -> {
             final ChronologyChangeListener listener = listenerRef.get();

             if (listener == null) {
                this.changeListeners.remove(listenerRef);
             } else {
                listener.handleCommit(commitRecord);
             }
          });
   }

   /**
    * Revert commit.
    *
    * @param conceptsToCommit the concepts to commit
    * @param conceptsToCheck the concepts to check
    * @param semanticsToCommit the semantics to commit
    * @param semanticsToCheck the semantics to check
    * @param pendingStampsForCommit the pending stamps for commit
    */
   protected void revertCommit(NidSet conceptsToCommit,
                               NidSet conceptsToCheck,
                               NidSet semanticsToCommit,
                               NidSet semanticsToCheck,
                               Map<UncommittedStamp, Integer> pendingStampsForCommit) {
      Get.stampService()
         .addPendingStampsForCommit(pendingStampsForCommit);
      this.uncommittedSequenceLock.lock();

      try {
         this.uncommittedConceptsWithChecksNidSet.or(conceptsToCheck);
         this.uncommittedConceptsNoChecksNidSet.or(conceptsToCommit);
         this.uncommittedConceptsNoChecksNidSet.andNot(conceptsToCheck);
         this.uncommittedSemanticsWithChecksNidSet.or(semanticsToCheck);
         this.uncommittedSemanticsNoChecksNidSet.or(semanticsToCommit);
         this.uncommittedSemanticsNoChecksNidSet.andNot(semanticsToCheck);
      } finally {
         this.uncommittedSequenceLock.unlock();
      }
   }

   /**
    * Check and write.
    *
    * @param cc the cc
    * @param checkers the checkers
    * @param alertCollection the alert collection
    * @param writeSemaphore the write semaphore
    * @param changeListeners the change listeners
    * @return the task
    */
   private Task<Void> checkAndWrite(ConceptChronology cc,
                                    ConcurrentSkipListSet<ChangeChecker> checkers,
                                    Semaphore writeSemaphore,
                                    ConcurrentSkipListSet<WeakReference<ChronologyChangeListener>> changeListeners) {
      writeSemaphore.acquireUninterruptibly();

      final WriteAndCheckConceptChronicle task = new WriteAndCheckConceptChronicle(
                                                     cc,
                                                           checkers,
                                                           writeSemaphore,
                                                           changeListeners,
                                                           (sememeOrConceptChronicle,
                                                            changeCheckerActive) -> handleUncommittedSequenceSet(
                                                                  sememeOrConceptChronicle,
                                                                        changeCheckerActive));

      this.writeCompletionService.submit(task);
      return task;
   }

   /**
    * Check and write.
    *
    * @param sc the sc
    * @param checkers the checkers
    * @param alertCollection the alert collection
    * @param writeSemaphore the write semaphore
    * @param changeListeners the change listeners
    * @return the task
    */
   private Task<Void> checkAndWrite(SemanticChronology sc,
                                    ConcurrentSkipListSet<ChangeChecker> checkers,
                                    Semaphore writeSemaphore,
                                    ConcurrentSkipListSet<WeakReference<ChronologyChangeListener>> changeListeners) {
      writeSemaphore.acquireUninterruptibly();

      final WriteAndCheckSemanticChronology task = new WriteAndCheckSemanticChronology(
                                                       sc,
                                                             checkers,
                                                             writeSemaphore,
                                                             changeListeners,
                                                             (sememeOrConceptChronicle,
                                                              changeCheckerActive) -> handleUncommittedSequenceSet(
                                                                    sememeOrConceptChronicle,
                                                                          changeCheckerActive));

      this.writeCompletionService.submit(task);
      return task;
   }

   /**
    * Defer nid action.
    *
    * @param nid the nid
    */
   private void deferNidAction(int nid) {
      Set<Integer> nids = this.deferredImportNoCheckNids.get();

      nids.add(nid);
   }

   /**
    * Handle uncommitted nid set.
    *
    * @param sememeOrConceptChronicle the sememe or concept chronicle
    * @param changeCheckerActive the change checker active
    */
   private void handleUncommittedSequenceSet(Chronology sememeOrConceptChronicle, boolean changeCheckerActive) {
      if (sememeOrConceptChronicle instanceof ObservableChronologyImpl) {
         sememeOrConceptChronicle = ((ObservableChronologyImpl) sememeOrConceptChronicle).getWrappedChronology();
      }

      try {
         this.uncommittedSequenceLock.lock();

         switch (sememeOrConceptChronicle.getIsaacObjectType()) {
         case CONCEPT: {
            final int    nid = sememeOrConceptChronicle.getNid();
            final NidSet set = changeCheckerActive ? this.uncommittedConceptsWithChecksNidSet
                  : this.uncommittedConceptsNoChecksNidSet;

            if (sememeOrConceptChronicle.isUncommitted()) {
               set.add(nid);
            } else {
               set.remove(nid);
            }

            break;
         }

         case SEMANTIC: {
            final int    nid = sememeOrConceptChronicle.getNid();
            final NidSet set = changeCheckerActive ? this.uncommittedSemanticsWithChecksNidSet
                  : this.uncommittedSemanticsNoChecksNidSet;

            if (sememeOrConceptChronicle.isUncommitted()) {
               set.add(nid);
            } else {
               set.remove(nid);
            }

            break;
         }

         default:
            throw new RuntimeException("Only Concepts or Sememes should be passed");
         }
      } finally {
         this.uncommittedSequenceLock.unlock();
      }
   }

   /**
    * Start me.
    */
   @PostConstruct
   private void startMe() {
      try {
         LOG.info("Starting CommitProvider post-construct at runlevel: " + LookupService.getCurrentRunLevel());
         this.writeCompletionService.start();

         if (this.loadRequired.get()) {
            LOG.info("Reading existing commit manager data. ");
            LOG.info("Reading " + COMMIT_MANAGER_DATA_FILENAME);

            try (DataInputStream in = new DataInputStream(
                                          new FileInputStream(
                                              new File(
                                                  this.commitManagerFolder.toFile(),
                                                  COMMIT_MANAGER_DATA_FILENAME)))) {
               this.databaseSequence.set(in.readLong());
               UuidIntMapMap.getNextNidProvider()
                            .set(in.readInt());
               this.uncommittedConceptsWithChecksNidSet.read(in);
               this.uncommittedConceptsNoChecksNidSet.read(in);
               this.uncommittedSemanticsWithChecksNidSet.read(in);
               this.uncommittedSemanticsNoChecksNidSet.read(in);
            }

            LOG.info("Reading: " + STAMP_ALIAS_MAP_FILENAME);
            this.stampAliasMap.read(new File(this.commitManagerFolder.toFile(), STAMP_ALIAS_MAP_FILENAME));
            LOG.info("Reading: " + STAMP_COMMENT_MAP_FILENAME);
            this.stampCommentMap.read(new File(this.commitManagerFolder.toFile(), STAMP_COMMENT_MAP_FILENAME));
            this.databaseValidity = DatabaseValidity.POPULATED_DIRECTORY;
         }
      } catch (final IOException e) {
         LookupService.getService(SystemStatusService.class)
                      .notifyServiceConfigurationFailure("Commit Provider", e);
         throw new RuntimeException(e);
      }
   }

   /**
    * Stop me.
    */
   @PreDestroy
   private void stopMe() {
      LOG.info("Stopping CommitProvider pre-destroy at runlevel: " + LookupService.getCurrentRunLevel());

      try {
         sync().get();
         this.pendingCommitTasks = null;
         this.writeCompletionService.stop();
      } catch (InterruptedException | ExecutionException ex) {
           LOG.error(ex);
       }
   }

   private void writeData() throws IOException {
      this.stampAliasMap.write(new File(this.commitManagerFolder.toFile(), STAMP_ALIAS_MAP_FILENAME));
      this.stampCommentMap.write(new File(this.commitManagerFolder.toFile(), STAMP_COMMENT_MAP_FILENAME));
      
      try (DataOutputStream out = new DataOutputStream(
              new FileOutputStream(
                      new File(
                              this.commitManagerFolder.toFile(),
                              COMMIT_MANAGER_DATA_FILENAME)))) {
         out.writeLong(this.databaseSequence.get());
         out.writeInt(UuidIntMapMap.getNextNidProvider()
                 .get());
         this.uncommittedConceptsWithChecksNidSet.write(out);
         this.uncommittedConceptsNoChecksNidSet.write(out);
         this.uncommittedSemanticsWithChecksNidSet.write(out);
         this.uncommittedSemanticsNoChecksNidSet.write(out);
      }
   }

   /**
    * Write.
    *
    * @param cc the cc
    * @param writeSemaphore the write semaphore
    * @param changeListeners the change listeners
    * @return the task
    */
   private Task<Void> write(ConceptChronology cc,
                            Semaphore writeSemaphore,
                            ConcurrentSkipListSet<WeakReference<ChronologyChangeListener>> changeListeners) {
      writeSemaphore.acquireUninterruptibly();

      final WriteConceptChronicle task = new WriteConceptChronicle(
                                             cc,
                                                   writeSemaphore,
                                                   changeListeners,
                                                   (sememeOrConceptChronicle,
                                                    changeCheckerActive) -> handleUncommittedSequenceSet(
                                                          sememeOrConceptChronicle,
                                                                changeCheckerActive));

      this.writeCompletionService.submit(task);
      return task;
   }

   /**
    * Write.
    *
    * @param sc the sc
    * @param writeSemaphore the write semaphore
    * @param changeListeners the change listeners
    * @return the task
    */
   private Task<Void> write(SemanticChronology sc,
                            Semaphore writeSemaphore,
                            ConcurrentSkipListSet<WeakReference<ChronologyChangeListener>> changeListeners) {
      writeSemaphore.acquireUninterruptibly();

      final WriteSemanticChronology task = new WriteSemanticChronology(
                                               sc,
                                                     writeSemaphore,
                                                     changeListeners,
                                                     (sememeOrConceptChronicle,
                                                      changeCheckerActive) -> handleUncommittedSequenceSet(
                                                            sememeOrConceptChronicle,
                                                                  changeCheckerActive));

      this.writeCompletionService.submit(task);
      return task;
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the aliases.
    *
    * @param stampSequence the stamp nid
    * @return the aliases
    */
   @Override
   public int[] getAliases(int stampSequence) {
      return this.stampAliasMap.getAliases(stampSequence);
   }

   /**
    * Gets the comment.
    *
    * @param stampSequence the stamp nid
    * @return the comment
    */
   @Override
   public Optional<String> getComment(int stampSequence) {
      return this.stampCommentMap.getComment(stampSequence);
   }

   //~--- set methods ---------------------------------------------------------

   /**
    * Set comment.
    *
    * @param stampSequence the stamp nid
    * @param comment the comment
    */
   @Override
   public void setComment(int stampSequence, String comment) {
      this.stampCommentMap.addComment(stampSequence, comment);
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the commit manager nid.
    *
    * @return the commit manager nid
    */
   @Override
   public long getCommitManagerSequence() {
      return this.databaseSequence.get();
   }

   @Override
   public UUID getDataStoreId() {
      return dataStoreId;
   }

   /**
    * Gets the database folder.
    *
    * @return the database folder
    */
   @Override
   public Path getDatabaseFolder() {
      return this.commitManagerFolder;
   }

   /**
    * Gets the database validity status.
    *
    * @return the database validity status
    */
   @Override
   public DatabaseValidity getDatabaseValidityStatus() {
      return this.databaseValidity;
   }

   /**
    * Gets the stamp alias stream.
    *
    * @return the stamp alias stream
    */
   @Override
   public Stream<StampAlias> getStampAliasStream() {
      return this.stampAliasMap.getStampAliasStream();
   }

   /**
    * Gets the stamp comment stream.
    *
    * @return the stamp comment stream
    */
   @Override
   public Stream<StampComment> getStampCommentStream() {
      return this.stampCommentMap.getStampCommentStream();
   }

   /**
    * Gets the uncommitted component text summary.
    *
    * @return the uncommitted component text summary
    */
   @Override
   public String getUncommittedComponentTextSummary() {
      final StringBuilder builder = new StringBuilder("CommitProvider summary: ");

      builder.append("\nuncommitted concepts with checks: ")
             .append(this.uncommittedConceptsWithChecksNidSet);
      builder.append("\nuncommitted concepts no checks: ")
             .append(this.uncommittedConceptsNoChecksNidSet);
      builder.append("\nuncommitted semantics with checks: ")
             .append(this.uncommittedSemanticsWithChecksNidSet);
      builder.append("\nuncommitted semantics no checks: ")
             .append(this.uncommittedSemanticsNoChecksNidSet);
      return builder.toString();
   }

   /**
    * Gets the uncommitted concept nids.
    *
    * @return the uncommitted concept nids
    */
   @Override
   public ObservableList<Integer> getUncommittedConceptNids() {
      // need to create a list that can be backed with a set...
      throw new UnsupportedOperationException(
          "Not supported yet.");  // To change body of generated methods, choose Tools | Templates.
   }

   //~--- inner classes -------------------------------------------------------

   /**
    * The Class ChangeListenerReference.
    */
   private static class ChangeListenerReference
           extends WeakReference<ChronologyChangeListener>
            implements Comparable<ChangeListenerReference> {
      /**
       * The listener uuid.
       */
      UUID listenerUuid;

      //~--- constructors -----------------------------------------------------

      /**
       * Instantiates a new change listener reference.
       *
       * @param referent the referent
       */
      public ChangeListenerReference(ChronologyChangeListener referent) {
         super(referent);
         this.listenerUuid = referent.getListenerUuid();
      }

      /**
       * Instantiates a new change listener reference.
       *
       * @param referent the referent
       * @param q the q
       */
      public ChangeListenerReference(ChronologyChangeListener referent,
                                     ReferenceQueue<? super ChronologyChangeListener> q) {
         super(referent, q);
         this.listenerUuid = referent.getListenerUuid();
      }

      //~--- methods ----------------------------------------------------------

      /**
       * Compare to.
       *
       * @param o the o
       * @return the int
       */
      @Override
      public int compareTo(ChangeListenerReference o) {
         return this.listenerUuid.compareTo(o.listenerUuid);
      }

      /**
       * Equals.
       *
       * @param obj the obj
       * @return true, if successful
       */
      @Override
      public boolean equals(Object obj) {
         if (obj == null) {
            return false;
         }

         if (getClass() != obj.getClass()) {
            return false;
         }

         final ChangeListenerReference other = (ChangeListenerReference) obj;

         return Objects.equals(this.listenerUuid, other.listenerUuid);
      }

      /**
       * Hash code.
       *
       * @return the int
       */
      @Override
      public int hashCode() {
         int hash = 3;

         hash = 67 * hash + Objects.hashCode(this.listenerUuid);
         return hash;
      }
   }
}

