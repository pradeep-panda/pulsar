/**
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
package org.apache.pulsar.common.policies.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.pulsar.common.naming.NamespaceName;
import org.apache.pulsar.common.policies.AutoFailoverPolicy;
import org.apache.pulsar.common.policies.NamespaceIsolationPolicy;
import org.apache.pulsar.common.policies.data.BrokerStatus;
import org.apache.pulsar.common.policies.data.NamespaceIsolationData;

import com.google.common.base.Objects;

public class NamespaceIsolationPolicyImpl implements NamespaceIsolationPolicy {

    private List<String> namespaces;
    private List<String> primary;
    private List<String> secondary;
    private AutoFailoverPolicy auto_failover_policy;

    private boolean matchNamespaces(String fqnn) {
        for (String nsRegex : namespaces) {
            if (fqnn.matches(nsRegex)) {
                return true;
            }
        }
        return false;
    }

    private List<URL> getMatchedBrokers(List<String> brkRegexList, List<URL> availableBrokers) {
        List<URL> matchedBrokers = new ArrayList<URL>();
        for (URL brokerUrl : availableBrokers) {
            if (this.matchesBrokerRegex(brkRegexList, brokerUrl.getHost())) {
                matchedBrokers.add(brokerUrl);
            }
        }
        return matchedBrokers;
    }

    public NamespaceIsolationPolicyImpl(NamespaceIsolationData policyData) {
        this.namespaces = policyData.namespaces;
        this.primary = policyData.primary;
        this.secondary = policyData.secondary;
        this.auto_failover_policy = AutoFailoverPolicyFactory.create(policyData.auto_failover_policy);
    }

    @Override
    public List<String> getPrimaryBrokers() {
        return this.primary;
    }

    @Override
    public List<String> getSecondaryBrokers() {
        return this.secondary;
    }

    @Override
    public List<URL> findPrimaryBrokers(List<URL> availableBrokers, NamespaceName namespace) {
        if (!this.matchNamespaces(namespace.toString())) {
            throw new IllegalArgumentException("Namespace " + namespace.toString() + " does not match policy");
        }
        // find the available brokers that matches primary brokers regex list
        return this.getMatchedBrokers(this.primary, availableBrokers);
    }

    @Override
    public List<URL> findSecondaryBrokers(List<URL> availableBrokers, NamespaceName namespace) {
        if (!this.matchNamespaces(namespace.toString())) {
            throw new IllegalArgumentException("Namespace " + namespace.toString() + " does not match policy");
        }
        // find the available brokers that matches primary brokers regex list
        return this.getMatchedBrokers(this.secondary, availableBrokers);
    }

    @Override
    public boolean shouldFallback(SortedSet<BrokerStatus> primaryBrokers) {
        // TODO Auto-generated method stub
        return false;
    }

    private boolean matchesBrokerRegex(List<String> brkRegexList, String broker) {
        for (String brkRegex : brkRegexList) {
            if (broker.matches(brkRegex)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPrimaryBroker(String broker) {
        return this.matchesBrokerRegex(this.primary, broker);
    }

    @Override
    public boolean isSecondaryBroker(String broker) {
        return this.matchesBrokerRegex(this.secondary, broker);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(namespaces, primary, secondary,
                auto_failover_policy);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NamespaceIsolationPolicyImpl) {
            NamespaceIsolationPolicyImpl other = (NamespaceIsolationPolicyImpl) obj;
            return Objects.equal(this.namespaces, other.namespaces) && Objects.equal(this.primary, other.primary)
                    && Objects.equal(this.secondary, other.secondary)
                    && Objects.equal(this.auto_failover_policy, other.auto_failover_policy);
        }

        return false;
    }

    @Override
    public SortedSet<BrokerStatus> getAvailablePrimaryBrokers(SortedSet<BrokerStatus> primaryCandidates) {
        SortedSet<BrokerStatus> availablePrimaries = new TreeSet<BrokerStatus>();
        for (BrokerStatus status : primaryCandidates) {
            if (this.auto_failover_policy.isBrokerAvailable(status)) {
                availablePrimaries.add(status);
            }
        }
        return availablePrimaries;
    }

    @Override
    public boolean shouldFailover(SortedSet<BrokerStatus> brokerStatus) {
        return this.auto_failover_policy.shouldFailoverToSecondary(brokerStatus);
    }

    public boolean shouldFailover(int totalPrimaryResourceUnits) {
        return this.auto_failover_policy.shouldFailoverToSecondary(totalPrimaryResourceUnits);
    }

    @Override
    public boolean isPrimaryBrokerAvailable(BrokerStatus brkStatus) {
        return this.isPrimaryBroker(brkStatus.getBrokerAddress())
                && this.auto_failover_policy.isBrokerAvailable(brkStatus);
    }

    @Override
    public String toString() {
        return String.format("namespaces=%s primary=%s secondary=%s auto_failover_policy=%s", namespaces, primary,
                secondary, auto_failover_policy);
    }
}
