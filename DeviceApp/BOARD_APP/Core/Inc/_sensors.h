#ifndef	SENSORS_H
#define	SENSORS_H

#ifdef __cplusplus
extern "C" {
#endif

#include "lsm6dsr.h"
#include "custom_mems_conf.h"
#include <stdio.h>

#define Sensors_int_1_port  (GPIOA)
#define Sensors_int_1_pin   (GPIO_PIN_3)
#define Sensors_int_2_port  (GPIOA)
#define Sensors_int_2_pin   (GPIO_PIN_1)

#define Sensors_read_I2C_address   (0xD7)
#define Sensors_write_I2C_address  (0xD6)

#define Sensors_bar_read_I2C_address   (0x00)
#define Sensors_bar_write_I2C_address  (0x00)

extern LSM6DSR_Object_t MotionSensor;
extern LSM6DSR_AxesRaw_t GYRO_axes;
extern LSM6DSR_AxesRaw_t ACC_axes;

void Sensors_Init();
void Sensors_PackageBuild(uint8_t *addr);

#ifdef __cplusplus
}
#endif
#endif
