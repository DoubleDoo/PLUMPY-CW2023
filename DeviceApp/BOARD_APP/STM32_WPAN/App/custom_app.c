/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file    App/custom_app.c
  * @author  MCD Application Team
  * @brief   Custom Example Application (Server)
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2023 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "app_common.h"
#include "dbg_trace.h"
#include "ble.h"
#include "custom_app.h"
#include "custom_stm.h"
#include "stm32_seq.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "_sensors.h"
//#include "lsm6dsr.h"
//#include "custom_mems_conf.h"
//#include <stdio.h>
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
typedef struct
{
  /* Sensors_service */
  uint8_t               Tmpv_Notification_Status;
  /* Battery_service */
  /* Control_service */
  /* USER CODE BEGIN CUSTOM_APP_Context_t */
  uint8_t TimerMeasurement_Id;
  uint8_t RebootReqCharHandl;
  /* USER CODE END CUSTOM_APP_Context_t */

  uint16_t              ConnectionHandle;
} Custom_App_Context_t;

/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private defines ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
#define MEASUREMENT_INTERVAL   (1000000/CFG_TS_TICK_VAL*2)/25
#define MEASUREMENT_BATTERY_INTERVAL   (1000000/CFG_TS_TICK_VAL*2)*30
/* USER CODE END PD */

/* Private macros -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
/**
 * START of Section BLE_APP_CONTEXT
 */

static Custom_App_Context_t Custom_App_Context;

/**
 * END of Section BLE_APP_CONTEXT
 */

uint8_t UpdateCharData[247];
uint8_t NotifyCharData[247];

/* USER CODE BEGIN PV */
uint8_t battery;
/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
/* Sensors_service */
static void Custom_Tmpv_Update_Char(void);
static void Custom_Tmpv_Send_Notification(void);
/* Battery_service */
/* Control_service */

/* USER CODE BEGIN PFP */
static void TempMeas( void );
/* USER CODE END PFP */

/* Functions Definition ------------------------------------------------------*/
void Custom_STM_App_Notification(Custom_STM_App_Notification_evt_t *pNotification)
{
  /* USER CODE BEGIN CUSTOM_STM_App_Notification_1 */

  /* USER CODE END CUSTOM_STM_App_Notification_1 */
  switch (pNotification->Custom_Evt_Opcode)
  {
    /* USER CODE BEGIN CUSTOM_STM_App_Notification_Custom_Evt_Opcode */

    /* USER CODE END CUSTOM_STM_App_Notification_Custom_Evt_Opcode */

    /* Sensors_service */
    case CUSTOM_STM_TMPV_NOTIFY_ENABLED_EVT:
      /* USER CODE BEGIN CUSTOM_STM_TMPV_NOTIFY_ENABLED_EVT */
    	 HW_TS_Stop(Custom_App_Context.TimerMeasurement_Id);
    	 HW_TS_Start(Custom_App_Context.TimerMeasurement_Id, MEASUREMENT_INTERVAL);
      /* USER CODE END CUSTOM_STM_TMPV_NOTIFY_ENABLED_EVT */
      break;

    case CUSTOM_STM_TMPV_NOTIFY_DISABLED_EVT:
      /* USER CODE BEGIN CUSTOM_STM_TMPV_NOTIFY_DISABLED_EVT */
    	 HW_TS_Stop(Custom_App_Context.TimerMeasurement_Id);
      /* USER CODE END CUSTOM_STM_TMPV_NOTIFY_DISABLED_EVT */
      break;

    /* Battery_service */
    case CUSTOM_STM_BRVL_READ_EVT:
      /* USER CODE BEGIN CUSTOM_STM_BRVL_READ_EVT */
    	Custom_Btrv_Update_Char();
      /* USER CODE END CUSTOM_STM_BRVL_READ_EVT */
      break;

    /* Control_service */
    case CUSTOM_STM_RERE_READ_EVT:
      /* USER CODE BEGIN CUSTOM_STM_RERE_READ_EVT */

      /* USER CODE END CUSTOM_STM_RERE_READ_EVT */
      break;

    case CUSTOM_STM_RERE_WRITE_NO_RESP_EVT:
      /* USER CODE BEGIN CUSTOM_STM_RERE_WRITE_NO_RESP_EVT */
    	if (pNotification->DataTransfered.pPayload[0] == 0x01)
    	  {
			  Custom_App_Context.RebootReqCharHandl=0x01;
			  HAL_GPIO_WritePin(GPIOB, GPIO_PIN_2, GPIO_PIN_SET);
			  UpdateCharData[0]=0x01;
			  Custom_STM_App_Update_Char(CUSTOM_STM_RERE, (uint8_t *)UpdateCharData);
		      *(uint32_t*)SRAM1_BASE = *(uint32_t*)pNotification->DataTransfered.pPayload;
		      NVIC_SystemReset();
		  }
		  if (pNotification->DataTransfered.pPayload[0] == 0x00)
		  {
			  Custom_App_Context.RebootReqCharHandl=0x00;
			  HAL_GPIO_WritePin(GPIOB, GPIO_PIN_2, GPIO_PIN_RESET);
			  UpdateCharData[0]=0x00;
			  Custom_STM_App_Update_Char(CUSTOM_STM_RERE, (uint8_t *)UpdateCharData);
		  }
      /* USER CODE END CUSTOM_STM_RERE_WRITE_NO_RESP_EVT */
      break;

    case CUSTOM_STM_REFU_READ_EVT:
      /* USER CODE BEGIN CUSTOM_STM_REFU_READ_EVT */

      /* USER CODE END CUSTOM_STM_REFU_READ_EVT */
      break;

    case CUSTOM_STM_REFU_WRITE_NO_RESP_EVT:
      /* USER CODE BEGIN CUSTOM_STM_REFU_WRITE_NO_RESP_EVT */

      /* USER CODE END CUSTOM_STM_REFU_WRITE_NO_RESP_EVT */
      break;

    default:
      /* USER CODE BEGIN CUSTOM_STM_App_Notification_default */
//    	HAL_GPIO_WritePin(GPIOB, GPIO_PIN_2, GPIO_PIN_SET);
      /* USER CODE END CUSTOM_STM_App_Notification_default */
      break;
  }
  /* USER CODE BEGIN CUSTOM_STM_App_Notification_2 */

  /* USER CODE END CUSTOM_STM_App_Notification_2 */
  return;
}

void Custom_APP_Notification(Custom_App_ConnHandle_Not_evt_t *pNotification)
{
  /* USER CODE BEGIN CUSTOM_APP_Notification_1 */

  /* USER CODE END CUSTOM_APP_Notification_1 */

  switch (pNotification->Custom_Evt_Opcode)
  {
    /* USER CODE BEGIN CUSTOM_APP_Notification_Custom_Evt_Opcode */

    /* USER CODE END P2PS_CUSTOM_Notification_Custom_Evt_Opcode */
    case CUSTOM_CONN_HANDLE_EVT :
      /* USER CODE BEGIN CUSTOM_CONN_HANDLE_EVT */

      /* USER CODE END CUSTOM_CONN_HANDLE_EVT */
      break;

    case CUSTOM_DISCON_HANDLE_EVT :
      /* USER CODE BEGIN CUSTOM_DISCON_HANDLE_EVT */

      /* USER CODE END CUSTOM_DISCON_HANDLE_EVT */
      break;

    default:
      /* USER CODE BEGIN CUSTOM_APP_Notification_default */

      /* USER CODE END CUSTOM_APP_Notification_default */
      break;
  }

  /* USER CODE BEGIN CUSTOM_APP_Notification_2 */

  /* USER CODE END CUSTOM_APP_Notification_2 */

  return;
}

void Custom_APP_Init(void)
{
  /* USER CODE BEGIN CUSTOM_APP_Init */
  battery=100;
  Custom_App_Context.RebootReqCharHandl=0x01;
  UpdateCharData[0]=0x01;
  Custom_STM_App_Update_Char(CUSTOM_STM_RERE, (uint8_t *)UpdateCharData);
  UpdateCharData[0]=battery;
  Custom_STM_App_Update_Char(CUSTOM_STM_BRVL, (uint8_t *)UpdateCharData);
  UTIL_SEQ_RegTask( 1<< CFG_TASK_MEAS_REQ_ID, UTIL_SEQ_RFU, Custom_Tmpv_Send_Notification );
  HW_TS_Create(CFG_TIM_PROC_ID_ISR, &(Custom_App_Context.TimerMeasurement_Id), hw_ts_Repeated, TempMeas);
  /* USER CODE END CUSTOM_APP_Init */
  return;
}

/* USER CODE BEGIN FD */

/* USER CODE END FD */

/*************************************************************
 *
 * LOCAL FUNCTIONS
 *
 *************************************************************/

/* Sensors_service */
void Custom_Tmpv_Update_Char(void) /* Property Read */
{
  uint8_t updateflag = 0;

  /* USER CODE BEGIN Tmpv_UC_1*/

  /* USER CODE END Tmpv_UC_1*/

  if (updateflag != 0)
  {
    Custom_STM_App_Update_Char(CUSTOM_STM_TMPV, (uint8_t *)UpdateCharData);
  }

  /* USER CODE BEGIN Tmpv_UC_Last*/

  /* USER CODE END Tmpv_UC_Last*/
  return;
}

void Custom_Tmpv_Send_Notification(void) /* Property Notification */
{
  uint8_t updateflag = 0;

  /* USER CODE BEGIN Tmpv_NS_1*/
  Sensors_PackageBuild(&NotifyCharData);
  updateflag = 1;
  /* USER CODE END Tmpv_NS_1*/

  if (updateflag != 0)
  {
    Custom_STM_App_Update_Char(CUSTOM_STM_TMPV, (uint8_t *)NotifyCharData);
  }

  /* USER CODE BEGIN Tmpv_NS_Last*/

  /* USER CODE END Tmpv_NS_Last*/

  return;
}

/* Battery_service */
/* Control_service */

/* USER CODE BEGIN FD_LOCAL_FUNCTIONS*/

void Custom_Btrv_Update_Char(void) /* Property Read */
{
  uint8_t updateflag = 0;

  /* USER CODE BEGIN Tmpv_UC_1*/
  battery--;
  UpdateCharData[0]=battery;
  updateflag = 1;
  /* USER CODE END Tmpv_UC_1*/

  if (updateflag != 0)
  {
    Custom_STM_App_Update_Char(CUSTOM_STM_BRVL, (uint8_t *)UpdateCharData);
  }

  /* USER CODE BEGIN Tmpv_UC_Last*/

  /* USER CODE END Tmpv_UC_Last*/
  return;
}

void Custom_Rere_Update_Char(void) /* Property Read */
{
  uint8_t updateflag = 0;

  /* USER CODE BEGIN Tmpv_UC_1*/
  battery--;
  UpdateCharData[0]=battery;
  updateflag = 1;
  /* USER CODE END Tmpv_UC_1*/

  if (updateflag != 0)
  {
    Custom_STM_App_Update_Char(CUSTOM_STM_BRVL, (uint8_t *)UpdateCharData);
  }

  /* USER CODE BEGIN Tmpv_UC_Last*/

  /* USER CODE END Tmpv_UC_Last*/
  return;
}

static void TempMeas( void )
{
  UTIL_SEQ_SetTask( 1<<CFG_TASK_MEAS_REQ_ID, CFG_SCH_PRIO_0);
  return;
}
/* USER CODE END FD_LOCAL_FUNCTIONS*/
