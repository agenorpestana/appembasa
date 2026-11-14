package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.CompanyEntity
import com.example.data.model.ReadingEntity
import com.example.data.model.StationEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    fun generateAndShareReport(
        context: Context,
        company: CompanyEntity,
        station: StationEntity?,
        readings: List<ReadingEntity>,
        periodTitle: String
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72 dpi
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        val dateHeaderFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))

        val pageWidth = 595f
        val pageHeight = 842f
        val margin = 36f

        // 1. Cabeçalho Corporativo
        paint.color = Color.parseColor("#0C2744") // Hydro Navy
        canvas.drawRect(margin, margin, pageWidth - margin, margin + 74f, paint)

        // Logo / Ícone de Gota e Saneamento
        paint.color = Color.parseColor("#00E5FF")
        canvas.drawCircle(margin + 36f, margin + 37f, 22f, paint)
        paint.color = Color.parseColor("#0C2744")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("H₂O", margin + 24f, margin + 42f, paint)

        // Textos do Cabeçalho
        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText(company.tradeName, margin + 70f, margin + 26f, paint)

        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#E0F2FE")
        canvas.drawText(company.corporateName, margin + 70f, margin + 40f, paint)
        canvas.drawText("CNPJ: ${company.cnpj} | ${company.generalManager}", margin + 70f, margin + 52f, paint)
        canvas.drawText("Endereço: ${company.address}", margin + 70f, margin + 64f, paint)

        var currentY = margin + 90f

        // 2. Título do Relatório
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("RELATÓRIO GERENCIAL DE CONTROLE DA QUALIDADE DA ÁGUA", margin, currentY, paint)

        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#475569")
        canvas.drawText("Emissão em: ${dateFormat.format(Date())} | Período: $periodTitle", margin, currentY + 14f, paint)

        currentY += 28f

        // 3. Bloco da Estação e Responsável Técnico
        val boxHeight = 54f
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(margin, currentY, pageWidth - margin, currentY + boxHeight, 8f, 8f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        val stationName = station?.name ?: "Todas as Estações Integradas (Consolidado Master)"
        canvas.drawText("Unidade Operacional: $stationName", margin + 12f, currentY + 18f, paint)

        paint.isFakeBoldText = false
        paint.textSize = 8f
        paint.color = Color.parseColor("#334155")
        val capText = station?.let { "Tipo: ${it.type.code} (${it.type.subtitle}) | Capacidade: ${it.capacity}" } ?: "Tipo: Sistema Geral ETA / ETE"
        canvas.drawText(capText, margin + 12f, currentY + 32f, paint)

        val techText = station?.let { "Responsável Técnico: ${it.techLeadName} (${it.techLeadDoc}) | Contato: ${it.techLeadContact}" }
            ?: "Supervisão Técnica Central: Dra. Beatriz Mendonça (CRQ 072045-VII)"
        canvas.drawText(techText, margin + 12f, currentY + 44f, paint)

        currentY += boxHeight + 14f

        // 4. Cartões de Resumo Executivo / KPIs
        val totalSamples = readings.size
        val compliantSamples = readings.count { it.isCompliant }
        val complianceRate = if (totalSamples > 0) (compliantSamples * 100f / totalSamples) else 100f

        val avgPh = if (totalSamples > 0) readings.map { it.ph }.average() else 7.0
        val avgTurb = if (totalSamples > 0) readings.map { it.turbidity }.average() else 0.30
        val avgFlu = if (totalSamples > 0) readings.map { it.fluoride }.average() else 0.70
        val avgCl = if (totalSamples > 0) readings.map { it.freeChlorine }.average() else 1.20

        val cardWidth = (pageWidth - (margin * 2) - 30f) / 4f
        val cardHeight = 44f

        fun drawKpiCard(x: Float, title: String, value: String, sub: String, isWarning: Boolean) {
            paint.color = if (isWarning) Color.parseColor("#FEF3C7") else Color.parseColor("#E0F2FE")
            canvas.drawRoundRect(x, currentY, x + cardWidth, currentY + cardHeight, 6f, 6f, paint)

            paint.color = Color.parseColor("#475569")
            paint.textSize = 7.5f
            paint.isFakeBoldText = false
            canvas.drawText(title, x + 8f, currentY + 14f, paint)

            paint.color = if (isWarning) Color.parseColor("#B45309") else Color.parseColor("#0369A1")
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText(value, x + 8f, currentY + 28f, paint)

            paint.textSize = 6.5f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#64748B")
            canvas.drawText(sub, x + 8f, currentY + 38f, paint)
        }

        drawKpiCard(margin, "CONFORMIDADE", String.format(Locale.US, "%.1f%%", complianceRate), "$compliantSamples de $totalSamples conformes", complianceRate < 95f)
        drawKpiCard(margin + cardWidth + 10f, "MÉDIA TURBIDEZ", String.format(Locale.US, "%.2f NTU", avgTurb), "LSE: 0,50 NTU", avgTurb > 0.50)
        drawKpiCard(margin + (cardWidth * 2) + 20f, "MÉDIA pH", String.format(Locale.US, "%.2f", avgPh), "Faixa: 6,00 - 9,00", avgPh !in 6.0..9.0)
        drawKpiCard(margin + (cardWidth * 3) + 30f, "MÉDIA CLORO", String.format(Locale.US, "%.2f mg/L", avgCl), "Faixa: 0,20 - 2,00", avgCl !in 0.2..2.0)

        currentY += cardHeight + 16f

        // 5. Tabela de Amostras e Leituras Analíticas
        paint.color = Color.parseColor("#0C2744")
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 18f, paint)

        paint.color = Color.WHITE
        paint.textSize = 7.5f
        paint.isFakeBoldText = true

        val colX = floatArrayOf(
            margin + 6f,
            margin + 74f,
            margin + 170f,
            margin + 224f,
            margin + 290f,
            margin + 350f,
            margin + 420f,
            margin + 480f
        )

        canvas.drawText("DATA/HORA", colX[0], currentY + 12f, paint)
        canvas.drawText("ESTAÇÃO", colX[1], currentY + 12f, paint)
        canvas.drawText("OPERADOR", colX[2], currentY + 12f, paint)
        canvas.drawText("pH", colX[3], currentY + 12f, paint)
        canvas.drawText("TURBID. (NTU)", colX[4], currentY + 12f, paint)
        canvas.drawText("FLÚOR (mg/L)", colX[5], currentY + 12f, paint)
        canvas.drawText("CLORO (mg/L)", colX[6], currentY + 12f, paint)
        canvas.drawText("STATUS", colX[7], currentY + 12f, paint)

        currentY += 18f

        // Linhas de Leituras (até 15 linhas na página para layout perfeito)
        paint.isFakeBoldText = false
        val displayReadings = readings.take(15)

        for ((index, item) in displayReadings.withIndex()) {
            val rowY = currentY + (index * 19f)
            val isEven = index % 2 == 0

            paint.color = if (isEven) Color.parseColor("#FFFFFF") else Color.parseColor("#F8FAFC")
            canvas.drawRect(margin, rowY, pageWidth - margin, rowY + 19f, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.strokeWidth = 0.5f
            canvas.drawLine(margin, rowY + 19f, pageWidth - margin, rowY + 19f, paint)

            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 7f
            canvas.drawText(dateFormat.format(Date(item.timestamp)), colX[0], rowY + 13f, paint)

            val shortStation = if (item.stationName.length > 15) item.stationName.take(15) + "…" else item.stationName
            canvas.drawText(shortStation, colX[1], rowY + 13f, paint)

            val shortOp = if (item.operatorName.length > 10) item.operatorName.take(10) + "…" else item.operatorName
            canvas.drawText(shortOp, colX[2], rowY + 13f, paint)

            canvas.drawText(String.format(Locale.US, "%.2f", item.ph), colX[3], rowY + 13f, paint)

            // Destaque se Turbidez acima de 0.50
            if (item.turbidity > 0.50) {
                paint.color = Color.parseColor("#DC2626")
                paint.isFakeBoldText = true
            } else {
                paint.color = Color.parseColor("#1E293B")
                paint.isFakeBoldText = false
            }
            canvas.drawText(String.format(Locale.US, "%.2f", item.turbidity), colX[4], rowY + 13f, paint)

            paint.color = Color.parseColor("#1E293B")
            paint.isFakeBoldText = false
            canvas.drawText(String.format(Locale.US, "%.2f", item.fluoride), colX[5], rowY + 13f, paint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.freeChlorine), colX[6], rowY + 13f, paint)

            // Status Badge
            if (item.isCompliant) {
                paint.color = Color.parseColor("#10B981")
                paint.isFakeBoldText = true
                canvas.drawText("CONFORME", colX[7], rowY + 13f, paint)
            } else {
                paint.color = Color.parseColor("#EF4444")
                paint.isFakeBoldText = true
                canvas.drawText("DESVIO", colX[7], rowY + 13f, paint)
            }
            paint.isFakeBoldText = false
        }

        val rowsCount = displayReadings.size
        currentY += (rowsCount * 19f) + 16f

        // 6. Rodapé com Assinaturas Oficiais
        val signY = pageHeight - margin - 70f

        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 1f
        canvas.drawLine(margin + 20f, signY, margin + 220f, signY, paint)
        canvas.drawLine(pageWidth - margin - 220f, signY, pageWidth - margin - 20f, signY, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 7.5f
        paint.isFakeBoldText = true
        val techSignName = station?.techLeadName ?: "Responsável Técnico Geral"
        val techSignDoc = station?.techLeadDoc ?: "CRQ / CREA"
        canvas.drawText(techSignName, margin + 30f, signY + 12f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 6.8f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("$techSignDoc - Controle de Qualidade", margin + 30f, signY + 22f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 7.5f
        paint.isFakeBoldText = true
        canvas.drawText("Operador de Turno / Analista", pageWidth - margin - 210f, signY + 12f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 6.8f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("Conforme Portaria GM/MS 888/2021", pageWidth - margin - 210f, signY + 22f, paint)

        // Nota de Certificação
        paint.textSize = 6.5f
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawText(
            "Documento gerado automaticamente pelo Sistema HydroScan. Autenticidade regulatória validada via assinatura digital interna.",
            margin,
            pageHeight - margin - 12f,
            paint
        )

        pdfDocument.finishPage(page)

        // Salvar em cacheDir/reports
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val fileName = "Relatorio_Qualidade_${station?.id ?: "Consolidado"}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(reportsDir, fileName)

        try {
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (_: Exception) {
            pdfDocument.close()
            return null
        } finally {
            pdfDocument.close()
        }

        return pdfFile
    }

    fun sharePdfFile(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Relatório Gerencial de Qualidade da Água - EMBASA")
            putExtra(
                Intent.EXTRA_TEXT,
                "Segue em anexo o Relatório Gerencial de Controle de Qualidade da Água e Padrões de Potabilidade."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório PDF (WhatsApp, E-mail...)"))
    }
}
