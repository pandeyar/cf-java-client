/*
 * Copyright 2013-2017 the original author or authors.
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

package org.cloudfoundry.operations.applications;

import org.cloudfoundry.Nullable;
import org.immutables.value.Value;

import java.time.Duration;

/**
 * The request options for the scale application operation
 */
@Value.Immutable
abstract class _ScaleApplicationRequest {

    /**
     * The disk limit in MB
     */
    @Nullable
    abstract Integer getDiskLimit();

    /**
     * The number of instances
     */
    @Nullable
    abstract Integer getInstances();

    /**
     * The memory limit in MB
     */
    @Nullable
    abstract Integer getMemoryLimit();

    /**
     * The name of the application
     */
    abstract String getName();

    /**
     * How long to wait for staging
     */
    @Nullable
    abstract Duration getStagingTimeout();

    /**
     * How long to wait for startup
     */
    @Nullable
    abstract Duration getStartupTimeout();

}
