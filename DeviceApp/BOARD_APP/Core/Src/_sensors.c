#include "_sensors.h"

LSM6DSR_Object_t MotionSensor;
LSM6DSR_AxesRaw_t GYRO_axes;
LSM6DSR_AxesRaw_t ACC_axes;
uint32_t counter;

void Sensors_Check();
void Sensors_Configure();
void Sensors_Init();


void Sensors_Init()
{
  counter=0;
  LSM6DSR_IO_t io_ctx;

  io_ctx.BusType     = LSM6DSR_I2C_BUS;
  io_ctx.Address     = Sensors_read_I2C_address;
  io_ctx.Init        = BSP_I2C1_Init;
  io_ctx.DeInit      = BSP_I2C1_DeInit;
  io_ctx.ReadReg     = BSP_I2C1_ReadReg;
  io_ctx.WriteReg    = BSP_I2C1_WriteReg;
  io_ctx.GetTick     = BSP_GetTick;

  LSM6DSR_RegisterBusIO(&MotionSensor, &io_ctx);

  LSM6DSR_Init(&MotionSensor);
  Sensors_Check();
  Sensors_Configure();
}

void Sensors_Configure()
{
  LSM6DSR_ACC_SetOutputDataRate(&MotionSensor, 52.0f);
  LSM6DSR_ACC_SetFullScale(&MotionSensor, 8);

  LSM6DSR_GYRO_SetOutputDataRate(&MotionSensor, 52.0f);
  LSM6DSR_GYRO_SetFullScale(&MotionSensor, 2000);

  LSM6DSR_ACC_GetAxesRaw(&MotionSensor, &ACC_axes);
  LSM6DSR_GYRO_GetAxesRaw(&MotionSensor, &GYRO_axes);

  LSM6DSR_ACC_Enable(&MotionSensor);
  LSM6DSR_GYRO_Enable(&MotionSensor);
}

void Sensors_Check()
{
  uint8_t id;
  if (LSM6DSR_ReadID(&MotionSensor, &id) != LSM6DSR_OK)
  {
	  Error_Handler();
  }
  if (id != LSM6DSR_ID) {
	  Error_Handler();
  }
}

void Sensors_ReadACC(uint8_t *addr)
{
  LSM6DSR_AxesRaw_t axes;
//  LSM6DSR_Axes_t taxes;
  LSM6DSR_ACC_GetAxesRaw(&MotionSensor,&axes);
//  LSM6DSR_ACC_GetAxes(&MotionSensor,&taxes);
  addr[0]=(uint8_t)(axes.x>>8);
  addr[1]=(uint8_t)(axes.x);
  addr[2]=(uint8_t)(axes.y>>8);
  addr[3]=(uint8_t)(axes.y);
  addr[4]=(uint8_t)(axes.z>>8);
  addr[5]=(uint8_t)(axes.z);
}

void Sensors_ReadGYRO(uint8_t *addr)
{
  LSM6DSR_AxesRaw_t axes;
//  LSM6DSR_Axes_t taxes;
  LSM6DSR_GYRO_GetAxesRaw(&MotionSensor,&axes);
//  LSM6DSR_GYRO_GetAxes(&MotionSensor,&taxes);
  addr[0]=(uint8_t)(axes.x>>8);
  addr[1]=(uint8_t)(axes.x);
  addr[2]=(uint8_t)(axes.y>>8);
  addr[3]=(uint8_t)(axes.y);
  addr[4]=(uint8_t)(axes.z>>8);
  addr[5]=(uint8_t)(axes.z);
}

void Sensors_ReadBARO(uint8_t *addr)
{
  addr[0]=0x00;
  addr[1]=0x00;
}

void Sensors_ReadTEMP(uint8_t *addr)
{
  addr[0]=0x00;
  addr[1]=0x00;
}

void Sensors_PackageBuild(uint8_t *addr)
{
  counter++;
  addr[0]=(uint8_t) (counter>>24);
  addr[1]=(uint8_t) (counter>>16);
  addr[2]=(uint8_t) (counter>>8);
  addr[3]=(uint8_t) (counter);
  Sensors_ReadACC(&addr[4]);
  Sensors_ReadGYRO(&addr[10]);
}

