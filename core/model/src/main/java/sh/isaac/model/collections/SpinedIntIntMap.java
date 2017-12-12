/*
 * Copyright 2017 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
         US Veterans Health Administration, OSHERA, and the Health Services Platform Consortium..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sh.isaac.model.collections;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import sh.isaac.model.ModelGet;

/**
 *
 * @author kec
 */
public class SpinedIntIntMap {

   private static final int DEFAULT_SPINE_SIZE = 1024;
   private final int spineSize;
   private final ConcurrentMap<Integer, AtomicIntegerArray> spines = new ConcurrentHashMap<>();
   private final int INITIALIZATION_VALUE = Integer.MAX_VALUE;

   public SpinedIntIntMap() {
      this.spineSize = DEFAULT_SPINE_SIZE;
   }

   public int sizeInBytes() {
      int sizeInBytes = 0;
      sizeInBytes = sizeInBytes + ((spineSize * 4) * spines.size()); // 4 bytes = bytes of 32 bit integer
      return sizeInBytes;
   }

   
   private AtomicIntegerArray newSpine(Integer spineKey) {
      int[] spine = new int[spineSize];
      Arrays.fill(spine, INITIALIZATION_VALUE);
      return new AtomicIntegerArray(spine);
   }
   
   public ConcurrentMap<Integer, AtomicIntegerArray> getSpines() {
      return spines;
   }

   public void put(int index, int element) {
      if (index < 0) {
         index = ModelGet.identifierService().getElementSequenceForNid(index);
      }
      int spineIndex = index / spineSize;
      int indexInSpine = index % spineSize;
      if (spineIndex > this.spines.size() + 2) {
         throw new IllegalStateException("Trying to add spine: " + spineIndex + " for: " + index);
      }
      this.spines.computeIfAbsent(spineIndex, this::newSpine).set(indexInSpine, element);
   }

   public int get(int index) {
      if (index < 0) {
         index = ModelGet.identifierService().getElementSequenceForNid(index);
      }
      int spineIndex = index / spineSize;
      int indexInSpine = index % spineSize;
      return this.spines.computeIfAbsent(spineIndex, this::newSpine).get(indexInSpine);
   }

   public int getAndUpdate(int index, IntUnaryOperator generator) {
       if (index < 0) {
         index = ModelGet.identifierService().getElementSequenceForNid(index);
      }
     int spineIndex = index / spineSize;
      int indexInSpine = index % spineSize;
      return this.spines.computeIfAbsent(spineIndex, this::newSpine).updateAndGet(indexInSpine, generator);
   }

   public boolean containsKey(int index) {
      if (index < 0) {
         index = ModelGet.identifierService().getElementSequenceForNid(index);
      }
      int spineIndex = index / spineSize;
      int indexInSpine = index % spineSize;
      return this.spines.computeIfAbsent(spineIndex, this::newSpine).get(indexInSpine) != INITIALIZATION_VALUE;
   }

   public void forEach(Processor processor) {
      int currentSpineCount = getSpineCount();
      int key = 0;
      for (int spineIndex = 0; spineIndex < currentSpineCount; spineIndex++) {
         AtomicIntegerArray spine = this.spines.computeIfAbsent(spineIndex, this::newSpine);
         for (int indexInSpine = 0; indexInSpine < spineSize; indexInSpine++) {
            int value = spine.get(indexInSpine);
            if (value != INITIALIZATION_VALUE) {
               processor.process(key, value);
            }
         }
         key++;
      }
   }
   
   private int getSpineCount() {
      int spineCount = 0;
      for (Integer spineKey:  spines.keySet()) {
         spineCount = Math.max(spineCount, spineKey + 1);
      }
      return spineCount; 
   }
   
   public IntStream keyStream() {
      final Supplier<? extends Spliterator.OfInt> streamSupplier = this.getKeySpliterator();

      return StreamSupport.intStream(streamSupplier, streamSupplier.get()
              .characteristics(), false);
   }

   public IntStream valueStream() {
      final Supplier<? extends Spliterator.OfInt> streamSupplier = this.getValueSpliterator();

      return StreamSupport.intStream(streamSupplier, streamSupplier.get()
              .characteristics(), false);
   }

   public interface Processor {

      public void process(int key, int value);
   }

   /**
    * Gets the value spliterator.
    *
    * @return the supplier<? extends spliterator. of int>
    */
   protected Supplier<? extends Spliterator.OfInt> getValueSpliterator() {
      return new ValueSpliteratorSupplier();
   }
  /**
    * Gets the value spliterator.
    *
    * @return the supplier<? extends spliterator. of int>
    */
   protected Supplier<? extends Spliterator.OfInt> getKeySpliterator() {
      return new KeySpliteratorSupplier();
   }

   /**
    * The Class KeySpliteratorSupplier.
    */
   private class KeySpliteratorSupplier
           implements Supplier<Spliterator.OfInt> {

      /**
       * Gets the.
       *
       * @return the spliterator of int
       */
      @Override
      public Spliterator.OfInt get() {
         return new SpinedKeySpliterator();
      }
   }

   /**
    * The Class ValueSpliteratorSupplier.
    */
   private class ValueSpliteratorSupplier
           implements Supplier<Spliterator.OfInt> {

      /**
       * Gets the.
       *
       * @return the spliterator of int
       */
      @Override
      public Spliterator.OfInt get() {
         return new SpinedValueSpliterator();
      }
   }

   private class SpinedValueSpliterator implements Spliterator.OfInt {
      int end;
      int currentPosition;
      
      public SpinedValueSpliterator() {
         this.end = DEFAULT_SPINE_SIZE * getSpineCount();
         this.currentPosition = 0;
      }
      
      public SpinedValueSpliterator(int start, int end) {
         this.currentPosition = start;
         this.end = end;
      }
      
      @Override
      public OfInt trySplit() {
         int splitEnd = end;
         int split = end - currentPosition;
         int half = split / 2;
         this.end = currentPosition + half;
         return new SpinedValueSpliterator(currentPosition + half + 1, splitEnd);
      }

      @Override
      public boolean tryAdvance(IntConsumer action) {
         while (currentPosition < end) {
            int value = get(currentPosition++);
            if (value != INITIALIZATION_VALUE) {
               action.accept(value);
               return true;
            }
         }
         return false;
      }

      @Override
      public long estimateSize() {
         return end - currentPosition;
      }

      @Override
      public int characteristics() {
          return Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED
                | Spliterator.SIZED;
     }

   }
   
   

   private class SpinedKeySpliterator implements Spliterator.OfInt {
      int end = DEFAULT_SPINE_SIZE * getSpineCount();
      int currentPosition = 0;

      public SpinedKeySpliterator() {
      }
      
      public SpinedKeySpliterator(int start, int end) {
         this.currentPosition = start;
         this.end = end;
      }
      
      @Override
      public Spliterator.OfInt trySplit() {
         int splitEnd = end;
         int split = end - currentPosition;
         int half = split / 2;
         this.end = currentPosition + half;
         return new SpinedValueSpliterator(currentPosition + half + 1, splitEnd);
      }

      @Override
      public boolean tryAdvance(IntConsumer action) {
         while (currentPosition < end) {
            int key = currentPosition++;
            int value = get(key);
            if (value != INITIALIZATION_VALUE) {
               action.accept(key);
               return true;
            }
         }
         return false;
      }

      @Override
      public long estimateSize() {
         return end - currentPosition;
      }

      @Override
      public int characteristics() {
          return Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED
                | Spliterator.SIZED | Spliterator.SORTED;
     }

   }   
}
