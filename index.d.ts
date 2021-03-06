declare module "react-native-sensors" {
  import { Observable } from "rxjs";

  type Sensors = {
    accelerometer: "accelerometer";
    gyroscope: "gyroscope";
    magnetometer: "magnetometer";
    barometer: "barometer";
    absoluterotationvector: "absoluterotationvector";
  };

  export const SensorTypes: Sensors;

  export const setUpdateIntervalForType: (
    type: keyof Sensors,
    updateInterval: number
  ) => void;

  interface SensorData {
    x: number;
    y: number;
    z: number;
    timestamp: string;
  }

  interface BarometerData {
    pressure: number;
  }

  type SensorsBase = {
    accelerometer: Observable<SensorData>;
    gyroscope: Observable<SensorData>;
    magnetometer: Observable<SensorData>;
    barometer: Observable<BarometerData>;
    absoluterotationvector: Observable<SensorData>;
  };

  export const {
    accelerometer,
    gyroscope,
    magnetometer,
    barometer,
    absoluterotationvector
  }: SensorsBase;

  const sensors: SensorsBase;

  export default sensors;
}
