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
 * Various states/events that can be captured in the {@link NumerusRollingNumber}.
 * <p>
 * Note that events are defined as different types:
 * <ul>
 * <li>Counter: <code>isCounter() == true</code></li>
 * <li>MaxUpdater: <code>isMaxUpdater() == true</code></li>
 * </ul>
 * <p>
 * The Counter type events can be used with {@link NumerusRollingNumber#increment}, {@link NumerusRollingNumber#add}, {@link NumerusRollingNumber#getRollingSum} and others.
 * <p>
 * The MaxUpdater type events can be used with {@link NumerusRollingNumber#updateRollingMax} and {@link NumerusRollingNumber#getRollingMaxValue}.
 */
public interface NumerusRollingNumberEvent {

    public boolean isCounter();

    public boolean isMaxUpdater();

    public NumerusRollingNumberEvent[] getValues();

    public int ordinal();

    public String name();
}