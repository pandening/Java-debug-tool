/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.javadebug.com.netflix.numerus;

/**
 * Generic interface to represent a property value so Hystrix can consume properties without being tied to any particular backing implementation.
 *
 * @param <T>
 *            Type of property value
 */
public interface NumerusProperty<T> {

    public T get();

    /**
     * Helper methods for wrapping static values.
     */
    public static class Factory {

        public static <T> NumerusProperty<T> asProperty(final T value) {
            return new NumerusProperty<T>() {

                @Override
                public T get() {
                    return value;
                }

            };
        }
    }
}
