package com.sunilshetty.clearspace.data.model

data class DiagnosticReading(val label: String, val value: String)
data class DeviceDiagnostics(val sections: Map<String, List<DiagnosticReading>>, val capturedAt: Long)
