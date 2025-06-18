package com.example.uberprofitabilityanalyzer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Bitmap // Placeholder for screen capture (Strategy 2)
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.uberprofitabilityanalyzer.model.TripDetails
import com.example.uberprofitabilityanalyzer.util.AppSettings
import java.security.MessageDigest
import java.util.Locale

class UberTripAccessibilityService : AccessibilityService() {

    private val TAG = "UberAccessibilitySvc"
    private val MAX_RECURSION_DEPTH = 10 // For node tree logging

    // Debouncing/State Management
    private var lastProcessedEventTime: Long = 0
    private var lastProcessedEventSignatureHash: String? = null // Using a hash for signature
    private val DEBOUNCE_INTERVAL_MS: Long = 2000 // 2 seconds

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val currentTime = System.currentTimeMillis()

        // Comprehensive Event Logging
        Log.d(TAG, "----------------------------------------------------------------")
        Log.i(TAG, "Event Received: type=${AccessibilityEvent.eventTypeToString(event.eventType)}, class=${event.className}, pkg=${event.packageName}, time=${event.eventTime}")

        val sourceNode: AccessibilityNodeInfo? = event.source
        if (sourceNode == null) {
            Log.w(TAG, "Event source node is null. Some details might be missing.")
            // Depending on the event type, source might be null.
            // For example, TYPE_NOTIFICATION_STATE_CHANGED might have a null source if the notification is dismissed.
            // if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && event.text.isNotEmpty()) {
            //     Log.i(TAG, "Notification text: ${event.text.joinToString("|")}")
            // }
            // return // Or proceed if other event data (like event.text) is useful
        } else {
            Log.i(TAG, "Source Node: class=${sourceNode.className}, viewId=${sourceNode.viewIdResourceName}, desc=${sourceNode.contentDescription}, text='${sourceNode.text}'")
            Log.d(TAG, "Logging Node Tree from Event Source (max depth $MAX_RECURSION_DEPTH):")
            logNodeTreeRecursive(sourceNode, 0)
            // sourceNode.recycle() // Recycle if it's the root and you are done.
        }


        // **1. Event Filtering & Uber App Identification**
        val uberDriverPackageName = "com.ubercab.driver" // Needs verification
        // val uberLitePackageName = "com.ubercab.uberlite" // If supporting Lite version
        // val packageNamesToMonitor = setOf(uberDriverPackageName, uberLitePackageName)
        // if (event.packageName?.toString() !in packageNamesToMonitor) {
        if (event.packageName?.toString() != uberDriverPackageName) { // Simplified for now
            // Log.v(TAG, "Event not from an expected Uber package. Ignoring.")
            return
        }

        // **2. Relevant Event Type Check**
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED && // Sometimes content appears after scroll
            event.eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED && // New elements might get focus
            event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED // Less likely for in-app popups, but possible
            ) {
            // Log.v(TAG, "Event type not typically relevant for new trip UI. Ignoring.")
            return
        }

        // **3. Debouncing & State Management**
        // Create a signature from key event properties.
        // For TYPE_WINDOW_CONTENT_CHANGED, the source node structure is important.
        val currentEventContent = StringBuilder()
        currentEventContent.append(AccessibilityEvent.eventTypeToString(event.eventType))
        sourceNode?.let { appendNodeDetailsForSignature(it, currentEventContent, 2) } // Limited depth for signature
        val currentEventSignatureHash = generateSha256(currentEventContent.toString())

        if (currentTime - lastProcessedEventTime < DEBOUNCE_INTERVAL_MS &&
            currentEventSignatureHash == lastProcessedEventSignatureHash) {
            Log.i(TAG, "Debounced: Similar event signature received within ${DEBOUNCE_INTERVAL_MS}ms. Hash: $currentEventSignatureHash. Ignoring.")
            return
        }
        Log.d(TAG, "New Event Signature Hash: $currentEventSignatureHash")


        // **4. Specific Trip Request Identification (Focus on Text Extraction from Nodes)**
        // This is the core logic to decide if this event represents a new trip.
        // It relies on the text extracted directly from nodes.

        val extractedTripText: String? = getTripTextFromNodes(event)

        if (extractedTripText != null && extractedTripText.isNotBlank()) {
            Log.i(TAG, "Potential Uber Trip Event DETECTED. Extracted Text for OCR/Parsing:\n---\n$extractedTripText\n---")

            lastProcessedEventTime = currentTime
            lastProcessedEventSignatureHash = currentEventSignatureHash

            // Load User Settings
            val userSettings = AppSettings.loadUserSettings(this)
            Log.d(TAG, "Loaded UserSettings: $userSettings")

            // **Strategy 1: Direct Text Parsing (from getTripTextFromNodes)**
            val tripDetails: TripDetails? = TextParserUtil.parseTextToTripDetails(extractedTripText)

            if (tripDetails != null) {
                Log.i(TAG, "Text parsed successfully (Strategy 1): $tripDetails")
                processTripDetails(tripDetails, userSettings)
            } else {
                Log.w(TAG, "Failed to parse TripDetails from directly extracted text (Strategy 1).")
                // **Strategy 2: Image-Based OCR (Conceptual Fallback)**
                // If direct text parsing fails or is insufficient, image-based OCR would be the next step.
                // This is NOT implemented here but documented.
                Log.d(TAG, "Conceptual Fallback: Attempting image-based OCR (Strategy 2 - NOT IMPLEMENTED).")
                // val bitmap: Bitmap? = captureScreenBitmap() // Placeholder for MediaProjection
                // if (bitmap != null) {
                //     TextRecognitionUtil.processImageForText(bitmap,
                //         onSuccess = { ocrText ->
                //             Log.i(TAG, "OCR Successful (Strategy 2). Recognized Text:\n$ocrText")
                //             val ocrTripDetails = TextParserUtil.parseTextToTripDetails(ocrText)
                //             if (ocrTripDetails != null) {
                //                 Log.i(TAG, "Text parsed successfully (Strategy 2 OCR): $ocrTripDetails")
                //                 processTripDetails(ocrTripDetails, userSettings)
                //             } else {
                //                 Log.w(TAG, "Failed to parse TripDetails from OCR text (Strategy 2).")
                //                 showOverlay(false, "Error: Could not parse trip details from screen (OCR).")
                //             }
                //         },
                //         onFailure = { e ->
                //             Log.e(TAG, "OCR Failed (Strategy 2).", e)
                //             showOverlay(false, "Error: Screen text recognition failed.")
                //         }
                //     )
                // } else {
                //     Log.w(TAG, "Bitmap capture failed (Strategy 2 - NOT IMPLEMENTED).")
                //     showOverlay(false, "Error: Screen capture failed.")
                // }
                // For now, if direct parsing fails, we show a generic error or do nothing.
                 showOverlay(false, "Error: Could not understand trip details.")
            }
        } else {
            Log.d(TAG, "No specific trip text extracted from this event's node structure.")
        }
         sourceNode?.recycle() // Important: Recycle the source node if you obtained it from event.getSource()
    }

    private fun processTripDetails(tripDetails: TripDetails, userSettings: com.example.uberprofitabilityanalyzer.model.UserSettings) {
        val isProfitable = ProfitabilityCalculator.isTripProfitable(tripDetails, userSettings)
        val earningsPerMin = if (tripDetails.timeInMinutes > 0) tripDetails.price / tripDetails.timeInMinutes else 0.0
        val resultString = String.format(
            Locale.US,
            "%s\n$%.2f | %.1fkm | %.0fmin\n$%.2f/min",
            if (isProfitable) "PROFITABLE" else "NOT PROFITABLE",
            tripDetails.price,
            tripDetails.distanceInKm,
            tripDetails.timeInMinutes,
            earningsPerMin
        )
        Log.i(TAG, "Profitability result: $resultString")
        showOverlay(isProfitable, resultString)
    }


    /**
     * STRATEGY 1: Direct Text Extraction from Accessibility Nodes.
     * Attempts to find and extract relevant trip information (price, distance, time)
     * by traversing the AccessibilityNodeInfo tree from the event source.
     * This method is preferred as it avoids screen capture and associated permissions/limitations.
     *
     * Relies heavily on the UI structure of the target application (Uber Driver app) being
     * somewhat consistent or having identifiable markers (View IDs, specific text patterns).
     *
     * @param event The accessibility event.
     * @return A concatenated string of potentially relevant text, or null if nothing significant is found.
     */
    private fun getTripTextFromNodes(event: AccessibilityEvent): String? {
        val sourceNode = event.source ?: return null
        val collectedText = StringBuilder()

        // Example: Look for specific keywords that often precede trip details.
        // This list needs to be refined by observing actual Uber app UI via logging.
        val keywords = listOf("fare", "trip", "pickup", "destination", "accept", "surge", "boost", "$", "km", "min", "request")

        // Recursive function to traverse nodes and collect text
        fun findTextRecursive(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > MAX_RECURSION_DEPTH) return // Avoid excessively deep searches

            var nodeText = ""
            if (node.text != null && node.text.toString().isNotEmpty()) {
                nodeText += node.text.toString() + " "
            }
            if (node.contentDescription != null && node.contentDescription.toString().isNotEmpty()) {
                nodeText += node.contentDescription.toString() + " "
            }

            // Heuristic: If the node text contains any keyword or looks like a number/price/distance
            if (nodeText.isNotBlank()) {
                 if (keywords.any { nodeText.toLowerCase(Locale.ROOT).contains(it) } || nodeText.matches(Regex(".*\\d.*"))) {
                    collectedText.append(nodeText.trim()).append(" | ") // Using " | " as a separator
                    Log.v(TAG, "getTripTextFromNodes: Appended text from node (class: ${node.className}, id: ${node.viewIdResourceName}): '$nodeText'")
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    findTextRecursive(child, depth + 1)
                    // child.recycle() // DO NOT recycle children obtained this way unless you are sure
                }
            }
        }

        Log.d(TAG, "Starting direct text extraction from nodes.")
        findTextRecursive(sourceNode, 0)
        // sourceNode.recycle() // Recycled in onAccessibilityEvent

        if (collectedText.isNotEmpty()) {
            return collectedText.toString()
        }
        return null
    }

    /**
     * STRATEGY 2: Image-Based OCR (Conceptual - NOT IMPLEMENTED HERE)
     * This function would be responsible for capturing the screen content as a Bitmap.
     *
     * How it would work (if direct node text extraction is insufficient):
     * 1. Permissions: Requires `SYSTEM_ALERT_WINDOW` (already declared) and potentially foreground service
     *    permission if used from a background service for extended periods.
     * 2. MediaProjection API: This is the standard Android API for screen capture since Lollipop (API 21).
     *    - It requires user consent for each capture session (typically once per app launch).
     *    - An Activity is usually needed to request `MediaProjectionManager.createScreenCaptureIntent()`.
     *    - The result (Intent) is then used to obtain a `MediaProjection` instance.
     *    - `VirtualDisplay` is created using the `MediaProjection` to render the screen to an `ImageReader`.
     *    - Images (Bitmaps) can then be acquired from the `ImageReader`.
     * 3. Challenges for AccessibilityService:
     *    - Starting an Activity from a service to get permission can be disruptive.
     *    - Managing the MediaProjection lifecycle from a service context is complex.
     *    - Continuous capture is resource-intensive. Capture should be triggered very selectively.
     *
     * If this strategy is pursued, the `TextRecognitionUtil.processImageForText(bitmap, ...)`
     * would be called with the captured bitmap.
     *
     * @return A Bitmap of the screen, or null.
     */
    private fun captureScreenBitmap(): Bitmap? {
        Log.w(TAG, "captureScreenBitmap() - STRATEGY 2: Image-based OCR screen capture is NOT IMPLEMENTED.")
        // Example: Return a dummy bitmap for testing the flow if needed by uncommenting next line
        // return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        return null
    }


    private fun showOverlay(isProfitable: Boolean, details: String) {
        Log.d(TAG, "Requesting to show overlay. Profitable: $isProfitable, Details: $details")
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW
            putExtra(OverlayService.EXTRA_IS_PROFITABLE, isProfitable)
            putExtra(OverlayService.EXTRA_DETAILS_TEXT, details)
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start OverlayService", e)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt: Service interrupted. Cleaning up.")
        lastProcessedEventSignatureHash = null // Reset debouncing state
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "onServiceConnected: Service connected and configured.")
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK // Listen to all events initially for discovery
            // Consider more specific types for production:
            // eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
            //              AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
            //              AccessibilityEvent.TYPE_VIEW_SCROLLED or
            //              AccessibilityEvent.TYPE_VIEW_FOCUSED // or AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED etc.

            // packageNames = arrayOf("com.ubercab.driver", "com.ubercab.uberlite") // Specify Uber packages
             packageNames = null // Monitor all for easier initial UI structure discovery phase

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            // notificationTimeout can be useful if relying on notification events.
            // flags:
            // FLAG_INCLUDE_NOT_IMPORTANT_VIEWS: Crucial for apps that might not mark all views as "important" for accessibility.
            // FLAG_REPORT_VIEW_IDS: To get resource names of views.
            // FLAG_RETRIEVE_INTERACTIVE_WINDOWS: To inspect window content.
            // FLAG_ENABLE_ACCESSIBILITY_VOLUME: If providing audio feedback.
            // FLAG_REQUEST_FILTER_KEY_EVENTS: If you need to intercept key events.
            flags = flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        setServiceInfo(serviceInfo) // Use setServiceInfo to apply changes
        Log.i(TAG, "UberTripAccessibilityService fully configured with info: $serviceInfo")
    }

    // Recursive function to log the node tree structure
    private fun logNodeTreeRecursive(nodeInfo: AccessibilityNodeInfo?, depth: Int) {
        if (nodeInfo == null || depth > MAX_RECURSION_DEPTH) {
            if (depth > MAX_RECURSION_DEPTH) Log.d(TAG, "${"  ".repeat(depth)}Max depth reached for this branch.")
            return
        }
        val padding = "  ".repeat(depth)
        val viewId = nodeInfo.viewIdResourceName ?: "N/A"
        val text = nodeInfo.text ?: ""
        val contentDesc = nodeInfo.contentDescription ?: ""
        Log.d(TAG, "$padding Node: class=${nodeInfo.className}, viewId=$viewId, text='$text', desc='$contentDesc', childCount=${nodeInfo.childCount}")

        for (i in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(i)
            logNodeTreeRecursive(child, depth + 1)
            child?.recycle() // Recycle children obtained via getChild
        }
    }

    // Helper to append node details for generating a signature (limited depth)
    private fun appendNodeDetailsForSignature(node: AccessibilityNodeInfo, builder: StringBuilder, depth: Int) {
        if (depth < 0) return
        builder.append(node.className).append(node.viewIdResourceName)
               .append(node.text).append(node.contentDescription)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                appendNodeDetailsForSignature(child, builder, depth - 1)
                // child.recycle() // Not recycling here as it's part of a larger traversal from event.source
            }
        }
    }

    // Helper to generate SHA-256 hash for event signatures
    private fun generateSha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating SHA-256 hash", e)
            input // Fallback to using the raw string if hashing fails
        }
    }
}
