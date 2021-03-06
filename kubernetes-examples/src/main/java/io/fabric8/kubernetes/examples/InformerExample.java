/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.examples;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class InformerExample {
  private static Logger logger = LoggerFactory.getLogger(InformerExample.class);

  public static void main(String args[]) throws IOException, InterruptedException {
    try (final KubernetesClient client = new DefaultKubernetesClient()) {
      SharedInformerFactory sharedInformerFactory = client.informers();
      SharedIndexInformer<Pod> podInformer = sharedInformerFactory.sharedIndexInformerFor(Pod.class, PodList.class, 15 * 60 * 1000);
      log("Informer factory initialized.");

      podInformer.addEventHandler(
        new ResourceEventHandler<Pod>() {
          @Override
          public void onAdd(Pod pod) {
            log(pod.getMetadata().getName() + " pod added\n");
          }

          @Override
          public void onUpdate(Pod oldPod, Pod newPod) {
            log(oldPod.getMetadata().getName() + " pod updated\n");
          }

          @Override
          public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
            log(pod.getMetadata().getName() + " pod deleted \n");
          }
        }
      );

      log("Starting all registered informers");
      sharedInformerFactory.startAllRegisteredInformers();
      Pod testPod = new PodBuilder()
        .withNewMetadata().withName("myapp-pod").withLabels(Collections.singletonMap("app", "myapp-pod")).endMetadata()
        .withNewSpec()
        .addNewContainer()
        .withName("myapp-container")
        .withImage("busybox:1.28")
        .withCommand("sh", "-c", "echo The app is running!; sleep 10")
        .endContainer()
        .addNewInitContainer()
        .withName("init-myservice")
        .withImage("busybox:1.28")
        .withCommand("sh", "-c", "echo inititalizing...; sleep 5")
        .endInitContainer()
        .endSpec()
        .build();

      client.pods().inNamespace("default").create(testPod);
      log("Pod created");
      Thread.sleep(3000L);

      Lister<Pod> podLister = new Lister<> (podInformer.getIndexer(), "default");
      Pod myPod = podLister.get("myapp-pod");
      log("PodLister has " + podLister.list().size());

      if (myPod != null) {
        log("***** myapp-pod created %s", myPod.getMetadata().getCreationTimestamp());
      }

      // Wait for some time now
      TimeUnit.MINUTES.sleep(15);

      sharedInformerFactory.stopAllRegisteredInformers();
    }
  }

  private static void log(String action, Object obj) {
    logger.info("{}: {}", action, obj);
  }

  private static void log(String action) {
    logger.info(action);
  }
}
