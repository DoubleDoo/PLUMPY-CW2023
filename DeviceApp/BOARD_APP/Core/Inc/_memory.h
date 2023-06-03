#ifndef	MEMORY_H
#define	MEMORY_H

#ifdef __cplusplus
extern "C" {
#endif

#include "stm32wbxx_hal.h"
#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>

#define M24512_Write_enable_port  (GPIOA)
#define M24512_Write_enable_pin   (GPIO_PIN_0)

#define M24512_read_I2C_address   (0xA6)
#define M24512_write_I2C_address  (0xA7)

#define	M24512_SIZE_KBIT		  (512)
#define	M24512_PSIZE_KBIT		  (1)


bool M24512_init(I2C_HandleTypeDef* i2cHandle);
bool M24512_isConnected();
bool M24512_read(uint16_t address, uint8_t *data, size_t len, uint32_t timeout);
bool M24512_test();

#ifdef __cplusplus
}
#endif
#endif
