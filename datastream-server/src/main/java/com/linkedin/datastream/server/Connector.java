package com.linkedin.datastream.server;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.linkedin.datastream.common.Datastream;

import java.util.List;

public interface Connector {
    void start();

    void stop();

    String getConnectorType();

    void onAssignmentChange(DatastreamContext context, List<DatastreamTask> tasks);

    /**
     * Provides a DatastreamTarget object with information of downstream
     * Kafka topic to which the connector will be producing change events.
     *
     * @param stream: Datastream model
     * @return populated DatastreamTarget
     */
    DatastreamTarget getDatastreamTarget(Datastream stream);

    /**
     * Validate the datastream. Datastream management service call this before writing the
     * Datastream into zookeeper. DMS ensureS that stream.source has sufficient details.
     * @param stream: Datastream model
     * @return validation result
     */
    DatastreamValidationResult validateDatastream(Datastream stream);
}