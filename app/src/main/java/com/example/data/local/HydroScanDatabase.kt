package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CompanyEntity::class,
        StationEntity::class,
        UserEntity::class,
        ReadingEntity::class,
        AlertEntity::class,
        VpsConfigEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(HydroScanConverters::class)
abstract class HydroScanDatabase : RoomDatabase() {

    abstract fun readingDao(): ReadingDao
    abstract fun stationDao(): StationDao
    abstract fun companyDao(): CompanyDao
    abstract fun userDao(): UserDao
    abstract fun alertDao(): AlertDao
    abstract fun vpsConfigDao(): VpsConfigDao

    companion object {
        private const val TAG = "HydroScanDatabase"

        @Volatile
        private var INSTANCE: HydroScanDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): HydroScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HydroScanDatabase::class.java,
                    "hydroscan_sanitation.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                // Inicializa sementes e super admin em background com segurança total
                scope.launch(Dispatchers.IO) {
                    try {
                        ensureInitialData(instance)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro seguro ao popular dados iniciais", e)
                    }
                }

                instance
            }
        }

        suspend fun ensureInitialData(db: HydroScanDatabase) {
            // 1. Super Admin Solicitado: suporte@unityautomacoes.com.br / senha 200616
            val existingAdmin = db.userDao().getUserByEmail("suporte@unityautomacoes.com.br")
            if (existingAdmin == null) {
                db.userDao().insertUser(
                    UserEntity(
                        id = "super_admin_unity",
                        name = "Super Admin (Unity Automações)",
                        email = "suporte@unityautomacoes.com.br",
                        passwordHash = "200616",
                        role = UserRole.MASTER,
                        stationId = null,
                        stationName = null,
                        registration = "ADM-MASTER-01"
                    )
                )
            }

            // 2. Empresa Base Operacional
            db.companyDao().insertOrUpdateCompany(
                CompanyEntity(
                    id = "empresa_default",
                    corporateName = "Unity Automações & Sistemas de Saneamento",
                    tradeName = "Unity Automações",
                    cnpj = "48.123.456/0001-90",
                    address = "Av. Industrial de Tratamento e Telemetria, Salvador - BA",
                    phone = "(71) 3000-2006",
                    email = "suporte@unityautomacoes.com.br",
                    generalManager = "Engenharia de Automação & Controle de Qualidade"
                )
            )

            // 3. Estações Iniciais Operacionais
            db.stationDao().insertStation(
                StationEntity(
                    id = "eta_central_01",
                    name = "ETA 01 - Estação Central",
                    type = StationType.ETA,
                    address = "Unidade Operacional Principal",
                    capacity = "1.500 L/s (5.400 m³/h)",
                    techLeadName = "Coordenação Técnica Unity",
                    techLeadDoc = "CRQ / CREA",
                    techLeadContact = "suporte@unityautomacoes.com.br",
                    isActive = true
                )
            )

            db.stationDao().insertStation(
                StationEntity(
                    id = "ete_bairro_novo",
                    name = "ETE Bairro Novo",
                    type = StationType.ETE,
                    address = "Unidade Sanitária Norte",
                    capacity = "850 L/s (3.060 m³/h)",
                    techLeadName = "Eng. de Meio Ambiente",
                    techLeadDoc = "CREA-BA 98765",
                    techLeadContact = "contato@unityautomacoes.com.br",
                    isActive = true
                )
            )

            // 4. Usuários de Demonstração (Gerente e Operador vinculados)
            if (db.userDao().getUserByEmail("gerente.eta@unityautomacoes.com.br") == null) {
                db.userDao().insertUser(
                    UserEntity(
                        id = "gerente_eta_01",
                        name = "Marcos Vinicius (Gerente)",
                        email = "gerente.eta@unityautomacoes.com.br",
                        passwordHash = "123456",
                        role = UserRole.MANAGER,
                        stationId = "eta_central_01",
                        stationName = "ETA 01 - Estação Central",
                        registration = "GER-ETA-01"
                    )
                )
            }

            if (db.userDao().getUserByEmail("operador.carlos@unityautomacoes.com.br") == null) {
                db.userDao().insertUser(
                    UserEntity(
                        id = "operador_eta_01",
                        name = "Carlos Eduardo (Operador)",
                        email = "operador.carlos@unityautomacoes.com.br",
                        passwordHash = "123456",
                        role = UserRole.OPERATOR,
                        stationId = "eta_central_01",
                        stationName = "ETA 01 - Estação Central",
                        registration = "OP-ETA-01"
                    )
                )
            }

            // 5. Configuração VPS Inicial (pronta para escalar)
            db.vpsConfigDao().insertOrUpdateVpsConfig(
                VpsConfigEntity(
                    id = "primary_vps",
                    serverUrl = "https://api.unityautomacoes.com.br/api/v1",
                    apiKey = "",
                    autoSyncEnabled = false,
                    lastSyncTimestamp = 0L,
                    lastSyncMessage = "Aguardando sincronização com a VPS"
                )
            )
        }
    }
}
