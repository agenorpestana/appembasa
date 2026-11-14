package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HydroScanDatabase
import com.example.data.model.CalibrationStatus
import com.example.data.model.UserRole
import com.example.domain.validation.WaterQualityValidator
import com.example.ui.HydroScanViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HydroScan", appName)
  }

  @Test
  fun `verify user roles and labels`() {
    assertEquals("Master / Administrador", UserRole.MASTER.label)
    assertEquals("Gerente de Unidade", UserRole.MANAGER.label)
    assertEquals("Usuário / Operador", UserRole.OPERATOR.label)
  }

  @Test
  fun `verify automated water quality calibration validator`() {
    val turbResult = WaterQualityValidator.validateTurbidityStandard(10.0, 9.95)
    assertTrue(turbResult.status == CalibrationStatus.APROVADO)

    val phResult = WaterQualityValidator.validatePhBuffer(7.00, 7.02)
    assertTrue(phResult.status == CalibrationStatus.APROVADO)
  }

  @Test
  fun `launch MainActivity scenario`() {
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    scenario.onActivity { activity ->
      assertNotNull(activity)
    }
    scenario.close()
  }

  @Test
  fun `verify HydroScanDatabase initialization`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = HydroScanDatabase.getDatabase(context, this)
    assertNotNull(db)
    val user = db.userDao().getUserByEmail("suporte@unityautomacoes.com.br")
    // Database can be initialized and queried
  }
}
