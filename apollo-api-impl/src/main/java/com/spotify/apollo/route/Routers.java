/*
 * -\-\-
 * Spotify Apollo API Implementations
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
 * --
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
 * -/-/-
 */
package com.spotify.apollo.route;

import com.spotify.apollo.Response;
import com.spotify.apollo.dispatch.Endpoint;

import java.util.stream.Stream;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.spotify.apollo.route.Middlewares.apolloDefaults;

public final class Routers {

  private static final Logger LOG = LoggerFactory.getLogger(Routers.class);

  private Routers() {
    // no instantiation
  }

  public static ApplicationRouter<Endpoint> newRouterFromInspecting(Object... objects) {
    return RuleRouter.of(rules(objects));
  }

  private static Stream<Rule<Endpoint>> toRules(Route<? extends AsyncHandler<Response<ByteString>>> route) {
    if ("GET".equals(route.method())) {
      final Route<? extends AsyncHandler<Response<ByteString>>>
          headRoute =
          route.copy("HEAD", route.uri(), route.handler(), route.docString().orElse(null));
      return Stream.of(route, headRoute).map(RouteRuleBuilder::toRule);
    } else {
      return Stream.of(route).map(RouteRuleBuilder::toRule);
    }
  }

  private static List<Rule<Endpoint>> rules(Object... objects) {
    final List<Rule<Endpoint>> rules = new ArrayList<>();

    for (Object object : objects) {
      if (object instanceof Route) {
        //noinspection unchecked
        ((Stream<Rule<Endpoint>>) toRules((Route) object)).forEachOrdered(rules::add);
      } else if (object instanceof RouteProvider) {
        ((RouteProvider) object).routes()
            .map(route -> route.withMiddleware(apolloDefaults()))
            .flatMap(Routers::toRules)
            .forEachOrdered(rules::add);
      } else {
        throw new IllegalArgumentException("Unknown route/rule instance detected " + object);
      }
    }

    return rules;
  }
}
