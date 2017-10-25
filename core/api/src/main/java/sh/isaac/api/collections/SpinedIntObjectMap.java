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
package sh.isaac.api.collections;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 *
 * @author kec
 * @param <E> the generic type for the spined list. 
 */
public class SpinedIntObjectMap<E>   {
   private static final int DEFAULT_SPINE_SIZE = 1024;
   private final int spineSize;
   private final ConcurrentMap<Integer, AtomicReferenceArray<E>> spines = new ConcurrentHashMap<>();
   private final AtomicInteger spineCount = new AtomicInteger();
   private Function<E,String> elementStringConverter;

   public void setElementStringConverter(Function<E, String> elementStringConverter) {
      this.elementStringConverter = elementStringConverter;
   }
   
   public void printToConsole() {
      if (elementStringConverter != null) {
         forEach((key, value) -> {
            System.out.println(key + ": " + elementStringConverter.apply(value));
         });
      } else {
         forEach((key, value) -> {
            System.out.println(key + ": " + value);
         });
      }
   }

   public SpinedIntObjectMap() {
      this.spineSize = DEFAULT_SPINE_SIZE;
   }
   
   private AtomicReferenceArray<E> newSpine(Integer spineKey) {
      spineCount.set(Math.max(spineKey + 1, spineCount.get()));
      AtomicReferenceArray<E> spine = new AtomicReferenceArray(spineSize);
      return spine;
   }

   public void put(int index, E element) {
      if (index < 0) {
         throw new ArrayIndexOutOfBoundsException("Index: " + index);
      }
      int spineIndex = index/spineSize;
      int indexInSpine = index % spineSize;
      this.spines.computeIfAbsent(spineIndex, this::newSpine).set(indexInSpine, element);
   }

   public E get(int index) {
      if (index < 0) {
         throw new ArrayIndexOutOfBoundsException("Index: " + index);
      }
      int spineIndex = index/spineSize;
      int indexInSpine = index % spineSize;
      return this.spines.computeIfAbsent(spineIndex, this::newSpine).get(indexInSpine);
   }

   public boolean containsKey(int index) {
      if (index < 0) {
         throw new ArrayIndexOutOfBoundsException("Index: " + index);
      }
      int spineIndex = index/spineSize;
      int indexInSpine = index % spineSize;
      return this.spines.computeIfAbsent(spineIndex, this::newSpine).get(indexInSpine) != null;
   }
   
   public void forEach(Processor<E> processor) {
      int currentSpineCount = spineCount.get();
      int key = 0;
      for (int spineIndex = 0; spineIndex < currentSpineCount; spineIndex++) {
         AtomicReferenceArray<E> spine = this.spines.computeIfAbsent(spineIndex, this::newSpine);
         for (int indexInSpine = 0; indexInSpine < spineSize; indexInSpine++) {
            E element = spine.get(indexInSpine);
            if (element != null) {
               processor.process(key, (E) element);
            }
            key++;
         }
         
      }
   } 
   public E accumulateAndGet(int index, E x, BinaryOperator<E> accumulatorFunction) {
      if (index < 0) {
         throw new ArrayIndexOutOfBoundsException("Index: " + index);
      }
      int spineIndex = index/spineSize;
      int indexInSpine = index % spineSize;
      return this.spines.computeIfAbsent(spineIndex, this::newSpine)
              .accumulateAndGet(indexInSpine, x, accumulatorFunction);
      
   }
   
   public interface Processor<E> {
      public void process(int key, E value);
   }
}
