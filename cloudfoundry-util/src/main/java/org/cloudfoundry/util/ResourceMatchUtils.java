/*
 * Copyright 2013-2016 the original author or authors.
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

package org.cloudfoundry.util;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.resourcematch.ListMatchingResourcesRequest;
import org.cloudfoundry.client.v2.resourcematch.ListMatchingResourcesResponse;
import org.cloudfoundry.client.v2.resourcematch.Resource;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.cloudfoundry.util.tuple.TupleUtils.function;

public final class ResourceMatchUtils {

    private ResourceMatchUtils() {
    }

    public static Mono<List<ResourceMetadata>> getMatchedResources(CloudFoundryClient cloudFoundryClient, Path application) {
        return getResources(application)
            .flatMap(ResourceMatchUtils::getMetadata)
            .collectList()
            .then(metadata -> Flux.fromIterable(metadata)
                .map(ResourceMatchUtils::toResource)
                .as(ResourceMatchUtils::toListMatchingResourceRequest)
                .then(request -> getMatchedHashes(cloudFoundryClient, request))
                .then(matchedHashes -> toMatchedResources(metadata, matchedHashes)));
    }

    private static Mono<String> getHash(Path resource) {
        return Mono.just(FileUtils.getSha1(resource));
    }

    private static Mono<Set<String>> getMatchedHashes(CloudFoundryClient cloudFoundryClient, ListMatchingResourcesRequest request) {
        return cloudFoundryClient.resourceMatch()
            .list(request)
            .map(ResourceMatchUtils::toMatchedHashes);
    }

    private static Mono<ResourceMetadata> getMetadata(Path resource) {
        return Mono
            .when(
                getHash(resource),
                getMode(resource),
                Mono.just(resource),
                getSize(resource)
            )
            .map(function(ResourceMatchUtils::toResourceMetadata));
    }

    private static Mono<String> getMode(Path resource) {
        return Mono.just(Integer.toOctalString(FileUtils.getUnixMode(resource)));
    }

    private static Flux<Path> getResources(Path root) {
        try {
            return Flux
                .fromStream(Files.walk(FileUtils.normalize(root)))
                .filter(Files::isRegularFile);
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

    private static Mono<Long> getSize(Path resource) {
        try {
            return Mono.just(Files.size(resource));
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

    private static Mono<ListMatchingResourcesRequest> toListMatchingResourceRequest(Flux<Resource> source) {
        return source
            .reduce(ListMatchingResourcesRequest.builder(), ListMatchingResourcesRequest.Builder::resource)
            .map(ListMatchingResourcesRequest.Builder::build);
    }

    private static Set<String> toMatchedHashes(ListMatchingResourcesResponse response) {
        return response.getResources().stream()
            .map(Resource::getHash)
            .collect(Collectors.toSet());
    }

    private static Mono<List<ResourceMetadata>> toMatchedResources(List<ResourceMetadata> metadata, Set<String> matchedHashes) {
        return Flux.fromStream(metadata.stream())
            .filter(m -> matchedHashes.contains(m.getHash()))
            .collectList();
    }

    private static Resource toResource(ResourceMetadata metadata) {
        return Resource.builder()
            .hash(metadata.getHash())
            .mode(metadata.getMode())
            .size(metadata.getSize())
            .build();
    }

    private static ResourceMetadata toResourceMetadata(String hash, String mode, Path resource, Long size) {
        return ResourceMetadata.builder()
            .hash(hash)
            .mode(mode)
            .resource(resource)
            .size(size)
            .build();
    }

}
