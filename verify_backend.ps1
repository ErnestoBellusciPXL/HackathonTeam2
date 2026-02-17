$ErrorActionPreference = "Stop"

function Test-Endpoint {
    param($Name, $Uri, $Method = "Get", $Body = $null, $Headers = @{}, $ExpectedStatus = 200)
    Write-Host "Testing $Name ($Uri)..." -NoNewline
    try {
        $params = @{
            Uri = $Uri
            Method = $Method
            Headers = $Headers
            ContentType = "application/json"
        }
        if ($Body) { $params.Body = ($Body | ConvertTo-Json -Depth 10) }
        
        $response = Invoke-RestMethod @params
        Write-Host " OK" -ForegroundColor Green
        return $response
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -eq $ExpectedStatus) {
             Write-Host " OK (Expected failure $status)" -ForegroundColor Green
             return $_.Exception.Response
        }
        Write-Host " FAILED ($status)" -ForegroundColor Red
        Write-Host $_.Exception.Message
        if ($_.Exception.Response) {
             $reader = New-Object System.IO.StreamReader $_.Exception.Response.GetResponseStream()
             Write-Host $reader.ReadToEnd()
        }
        return $null
    }
}

# 1. Health Check
Test-Endpoint "Backend Health" "http://localhost:8080/actuator/health"

# 2. Login
Write-Host "`nLogging in..."
$loginBody = @{
    email = "admin@openinzicht.be"
    password = "0penInzicht?1PXL"
    rememberMe = $false
}
$auth = Test-Endpoint "Login" "http://localhost:8080/api/auth/login" "Post" $loginBody
$token = $auth.token
$headers = @{ Authorization = "Bearer $token" }

if (-not $token) {
    Write-Error "Login failed, cannot proceed."
}

# 3. AI Generation
Write-Host "`nTesting AI Generation..."
$genBody = @{
    inputs = @(
        @{ question = "Wie ben je?"; answer = "Jan Janssens" },
        @{ question = "Wat is er gebeurd?"; answer = "Ik heb een prijs gewonnen" },
        @{ question = "Waar?"; answer = "In Brussel" },
        @{ question = "Wanneer?"; answer = "Gisteren" },
        @{ question = "Hoe voelde je je?"; answer = "Blij" }
    )
}
# Note: This might take a while, AI is slow.
$genResponse = Test-Endpoint "Generate Story" "http://localhost:8080/api/stories/ai/generate" "Post" $genBody $headers

if ($genResponse -and $genResponse.reformattedStoryContent) {
    $storyText = $genResponse.reformattedStoryContent
    Write-Host "Generated Story Preview: $($storyText.substring(0, [math]::Min(100, $storyText.Length)))..." -ForegroundColor Cyan
}

# 4. AI Validation
Write-Host "`nTesting AI Validation..."
$valBody = @{
    title = "Mijn Prijs"
    content = "ik heb een prijs gewonen in brussel gisteren en ik was kapot blij."
}
$validation = Test-Endpoint "Validate Story" "http://localhost:8080/api/stories/ai/validate" "Post" $valBody $headers

if ($validation) {
    Write-Host "Validation Result:"
    Write-Host "Fixed Title: $($validation.fixedTitle)" -ForegroundColor Cyan
    Write-Host "Fixed Content: $($validation.fixedContent)" -ForegroundColor Cyan
    Write-Host "Changes: $($validation.changes | ForEach-Object { $_.reason })" -ForegroundColor Cyan
}
