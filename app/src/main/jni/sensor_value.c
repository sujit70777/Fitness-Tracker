/*
 * Copyright (C) 2015 The Android Open Source Project
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
// This file contains an example app that uses sensors NDK API.
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <map>
#include <string>
#include <android/looper.h>
#include <android/sensor.h>
#include <hardware/sensors.h>
const char kPackageName[] = "ndk-example-app";
const int kLooperId = 1;
int main(int argc, char* argv[]) {
  ASensorManager* sensor_manager =
      ASensorManager_getInstanceForPackage(kPackageName);
  if (!sensor_manager) {
    fprintf(stderr, "Failed to get a sensor manager\n");
    return 1;
  }
  ASensorList sensor_list;
  int sensor_count = ASensorManager_getSensorList(sensor_manager, &sensor_list);
  printf("Found %d sensors\n", sensor_count);
  for (int i = 0; i < sensor_count; i++) {
    printf("Found %s\n", ASensor_getName(sensor_list[i]));
  }
  ASensorEventQueue* queue = ASensorManager_createEventQueue(
      sensor_manager,
      ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS),
      kLooperId, NULL /* no callback */, NULL /* no data */);
  if (!queue) {
    fprintf(stderr, "Failed to create a sensor event queue\n");
    return 1;
  }
  const std::map<int, std::string> kSensorSamples = {
      /*
       * Accelerometer:
       *   Reporting mode: continuous. Events are generated continuously.
       */
      { SENSOR_TYPE_ACCELEROMETER, "accelerometer" },
      /*
       * Proximity sensor:
       *   Reporting mode: on-change. Events are generated only when proximity
       *     value has changed.
       */
      { SENSOR_TYPE_PROXIMITY, "proximity sensor" },
      /*
       * Significant motion sensor:
       *   Reporting mode: one-shot. An event is generated when significant
       *     motion is detected. After that, the sensor will be disabled by
       *     itself.
       */
      { SENSOR_TYPE_SIGNIFICANT_MOTION, "significant motion sensor" },
  };
  const int kNumSamples = 10;
  const int kNumEvents = 1;
  const int kTimeoutMilliSecs = 1000;
  const int kWaitTimeSecs = 1;
  for (auto& sensor_type : kSensorSamples) {
    const ASensor* sensor = ASensorManager_getDefaultSensor(sensor_manager,
                                                            sensor_type.first);
    if (sensor && !ASensorEventQueue_enableSensor(queue, sensor)) {
      for (int i = 0; i < kNumSamples; i++) {
        int ident = ALooper_pollAll(kTimeoutMilliSecs,
                                    NULL /* no output file descriptor */,
                                    NULL /* no output event */,
                                    NULL /* no output data */);
        if (ident == kLooperId) {
          ASensorEvent data;
          if (ASensorEventQueue_getEvents(queue, &data, kNumEvents)) {
            if (sensor_type.first == SENSOR_TYPE_ACCELEROMETER) {
              printf("Acceleration: x = %f, y = %f, z = %f\n",
                     data.acceleration.x, data.acceleration.y,
                     data.acceleration.z);
            } else if (sensor_type.first == SENSOR_TYPE_PROXIMITY) {
              printf("Proximity distance: %f\n", data.distance);
            } else if (sensor_type.first == SENSOR_TYPE_SIGNIFICANT_MOTION) {
              if (data.data[0] == 1) {
                printf("Significant motion detected\n");
                break;
              }
            }
          }
        }
        sleep(kWaitTimeSecs);
      }
      int ret = ASensorEventQueue_disableSensor(queue, sensor);
      if (ret) {
        fprintf(stderr, "Failed to disable %s: %s\n",
                sensor_type.second.c_str(), strerror(ret));
      }
    } else {
      fprintf(stderr, "No %s found or failed to enable it\n",
              sensor_type.second.c_str());
    }
  }
  int ret = ASensorManager_destroyEventQueue(sensor_manager, queue);
  if (ret) {
    fprintf(stderr, "Failed to destroy event queue: %s\n", strerror(ret));
    return 1;
  }
  return 0;
}