package com.example.data.model

enum class UserRole(val label: String, val shortLabel: String) {
    MASTER("Master / Administrador", "Master"),
    MANAGER("Gerente de Unidade", "Gerente"),
    OPERATOR("Usuário / Operador", "Operador")
}

enum class StationType(val code: String, val label: String, val subtitle: String) {
    ETA("ETA", "Estação de Tratamento de Água", "Água Potável"),
    ETE("ETE", "Estação de Tratamento de Esgoto", "Efluentes / Sanitário")
}

enum class CalibrationStatus(val label: String, val isPassed: Boolean) {
    APROVADO("Aprovado", true),
    REPROVADO("Reprovado", false),
    NAO_APLICAVEL("N/A", true)
}

enum class AlertLevel(val label: String) {
    NORMAL("Normal"),
    ATENCAO("Atenção"),
    FORA_DO_LIMITE("Fora do Limite de Aceitação")
}

enum class ReadingSource(val label: String) {
    IOT_TELEMETRIA("Sensor IoT Automático"),
    MANUAL_VERIFICADO("Coleta Manual Verificada")
}

enum class SyncStatus(val label: String) {
    SYNCED("Sincronizado"),
    PENDING("Pendente de Envio"),
    FAILED("Falha no Envio")
}
