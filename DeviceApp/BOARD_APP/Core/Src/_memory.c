#include <_memory.h>


I2C_HandleTypeDef* M24512_I2C_handler;

bool M24512_init(I2C_HandleTypeDef* i2cHandle)
{
	M24512_I2C_handler=i2cHandle;
	HAL_GPIO_WritePin(M24512_Write_enable_port,M24512_Write_enable_pin,GPIO_PIN_SET);
}



bool M24512_isConnected()
{
  HAL_GPIO_WritePin(M24512_Write_enable_port,M24512_Write_enable_pin,GPIO_PIN_RESET);
  if (HAL_I2C_IsDeviceReady(M24512_I2C_handler, M24512_read_I2C_address, 2, 10)==HAL_OK)
  {
	    if (HAL_I2C_IsDeviceReady(M24512_I2C_handler, M24512_write_I2C_address, 2, 10)==HAL_OK)
	    {
	      HAL_GPIO_WritePin(M24512_Write_enable_port,M24512_Write_enable_pin,GPIO_PIN_SET);
	      return true;
	    }
	    else
	    {
	      HAL_GPIO_WritePin(M24512_Write_enable_port,M24512_Write_enable_pin,GPIO_PIN_SET);
	      return false;
	    }
  }
  else
  {
	HAL_GPIO_WritePin(M24512_Write_enable_port,M24512_Write_enable_pin,GPIO_PIN_SET);
    return false;
  }
}


bool M24512_read(uint16_t address, uint8_t *data, size_t len, uint32_t timeout){
	  HAL_GPIO_WritePin(M24512_Write_enable_port,M24512_Write_enable_pin,GPIO_PIN_SET);
	  if (HAL_I2C_Mem_Read(M24512_I2C_handler, M24512_read_I2C_address, address, I2C_MEMADD_SIZE_16BIT, data, len, timeout) == HAL_OK)
	  {
	    return true;
	  }
	  else
	  {
	    return false;
	  }
}

bool M24512_write (uint16_t address, uint8_t *data, size_t len, uint32_t timeout){

	  uint16_t w;
	  uint32_t startTime = HAL_GetTick();
	  HAL_GPIO_WritePin(M24512_Write_enable_port,M24512_Write_enable_pin,GPIO_PIN_RESET);
	  while (1)
	  {
	    w = 0x80 - (address  % 0x80);
	    if (w > len)
	      w = len;

	    if (HAL_I2C_Mem_Write(M24512_I2C_handler, M24512_write_I2C_address, address, I2C_MEMADD_SIZE_16BIT, data, w, 100) == HAL_OK)
	    {
	      HAL_Delay(10);
	      len -= w;
	      data += w;
	      address += w;
	      if (len == 0)
	      {
	    	HAL_GPIO_WritePin(M24512_Write_enable_port,M24512_Write_enable_pin,GPIO_PIN_SET);
	        return true;
	      }
	      if (HAL_GetTick() - startTime >= timeout)
	      {
	        return false;
	      }
	    }
	    else
	    {
	      HAL_GPIO_WritePin(M24512_Write_enable_port,M24512_Write_enable_pin,GPIO_PIN_SET);
	      return false;
	    }
	  }
}

bool M24512_erase(void){
//	 const uint8_t eraseData[32] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF\
//	    , 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
//	  uint32_t bytes = 0;
//	  while ( bytes < (_EEPROM_SIZE_KBIT * 256))
//	  {
//	    if (ee24_write(bytes, (uint8_t*)eraseData, sizeof(eraseData), 100) == false)
//	      return false;
//	    bytes += sizeof(eraseData);
//	  }
	  return true;
}

bool M24512_test()
{
	bool res=true;
	if(!M24512_isConnected()){
		res=false;
	  }
	  uint8_t test=38;
	  if(!M24512_write(0xf000,&test, 1, 500)){
		  res=false;
	  }
	  uint8_t read;
	  if(!M24512_read(0xf000, &read, 1, 500)){
		  res=false;
	  }
	  if(test!=read)
	  {
		  res=false;
	  }
	  return res;
}
