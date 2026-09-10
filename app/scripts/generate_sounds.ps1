param(
    [string]$OutDir = "res\raw"
)

$sampleRate = 22050
$out = (Join-Path $PSScriptRoot $OutDir)
New-Item -ItemType Directory -Path $out -Force | Out-Null

function New-WavFile {
    param([string]$Path, [double[]]$Samples)
    $count = $Samples.Length
    $byteCount = $count * 2
    $fs = [System.IO.File]::Create($Path)
    $bw = New-Object System.IO.BinaryWriter($fs)
    try {
        $bw.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF"))
        $bw.Write([int32](36 + $byteCount))
        $bw.Write([System.Text.Encoding]::ASCII.GetBytes("WAVE"))
        $bw.Write([System.Text.Encoding]::ASCII.GetBytes("fmt "))
        $bw.Write([int32]16)                     # fmt chunk size
        $bw.Write([int16]1)                      # PCM
        $bw.Write([int16]1)                      # mono
        $bw.Write([int32]$sampleRate)
        $bw.Write([int32]($sampleRate * 2))      # byte rate
        $bw.Write([int16]2)                      # block align
        $bw.Write([int16]16)                     # bits per sample
        $bw.Write([System.Text.Encoding]::ASCII.GetBytes("data"))
        $bw.Write([int32]$byteCount)
        foreach ($s in $Samples) {
            $clamped = [Math]::Max(-1.0, [Math]::Min(1.0, $s))
            $bw.Write([int16]([Math]::Round($clamped * 32767)))
        }
    } finally {
        $bw.Close()
        $fs.Close()
    }
}

function New-NoteSamples {
    param([double]$Freq, [double]$Duration, [double]$Attack = 0.02, [double]$Gain = 0.55)
    $n = [int]($sampleRate * $Duration)
    $samples = New-Object double[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        # attack / decay envelope
        $env = 1.0
        if ($t -lt $Attack) { $env = $t / $Attack }
        $decay = [Math]::Exp(-2.2 * $t)
        $s = 0.0
        $s += [Math]::Sin(2 * [Math]::PI * $Freq * $t)
        $s += 0.25 * [Math]::Sin(2 * [Math]::PI * $Freq * 2 * $t)   # octave harmonic
        $s += 0.12 * [Math]::Sin(2 * [Math]::PI * $Freq * 3 * $t)   # 3rd harmonic
        $samples[$i] = $s * $env * $decay * $Gain
    }
    return ,$samples
}

# ---- sound_chime.wav : two gentle chimes (E5 -> A5) ----
$a = New-NoteSamples -Freq 659.25 -Duration 0.9 -Gain 0.5
$b = New-NoteSamples -Freq 880.00 -Duration 1.2 -Gain 0.45
$gap = New-Object double[] ([int]($sampleRate * 0.35))
$chime = $a + $gap + $b
New-WavFile -Path (Join-Path $out "sound_chime.wav") -Samples $chime

# ---- sound_call.wav : soft ascending call (C5 E5 G5 C6) ----
$c1 = New-NoteSamples -Freq 523.25 -Duration 0.5 -Gain 0.42
$e1 = New-NoteSamples -Freq 659.25 -Duration 0.5 -Gain 0.42
$g1 = New-NoteSamples -Freq 783.99 -Duration 0.5 -Gain 0.42
$c2 = New-NoteSamples -Freq 1046.50 -Duration 1.2 -Gain 0.4
$g2 = New-Object double[] ([int]($sampleRate * 0.18))
$call = $c1 + $g2 + $e1 + $g2 + $g1 + $g2 + $c2
New-WavFile -Path (Join-Path $out "sound_call.wav") -Samples $call

# ---- sound_dawn.wav : long soft sustained tone (G4) ----
$d = New-NoteSamples -Freq 392.00 -Duration 2.8 -Gain 0.5
New-WavFile -Path (Join-Path $out "sound_dawn.wav") -Samples $d

Write-Output "Generated WAV files in $out"
Get-ChildItem $out | Select-Object Name, Length
