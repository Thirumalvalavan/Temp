package com.thiru.temp.temp

import android.app.assist.AssistContent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.thiru.temp.temp.home.FitStatsFragment
import com.thiru.temp.temp.model.FitActivity
import com.thiru.temp.temp.model.FitRepository
import com.thiru.temp.temp.tracking.FitTrackingFragment
import com.thiru.temp.temp.tracking.FitTrackingService
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.appindexing.builders.AssistActionBuilder
import org.json.JSONObject

/**
 * Main activity responsible for the app navigation and handling deep-links.
 */
class MainActivity :
    AppCompatActivity(), FitStatsFragment.FitStatsActions, FitTrackingFragment.FitTrackingActions {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Handle the intent this activity was launched with.
        intent?.handleIntent()
    }

    /**
     * Handle new intents that are coming while the activity is on foreground since we set the
     * launchMode to be singleTask, avoiding multiple instances of this activity to be created.
     *
     * See [launchMode](https://developer.android.com/guide/topics/manifest/activity-element#lmode)
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.handleIntent()
    }

    /**
     * When a fragment is attached add the required callback methods.
     */
    override fun onAttachFragment(fragment: Fragment) {
        when (fragment) {
            is FitStatsFragment -> fragment.actionsCallback = this
            is FitTrackingFragment -> fragment.actionsCallback = this
        }
    }

    /**
     * When the user invokes an App Action while in your app, users will see a suggestion
     * to share their foreground content.
     *
     * By implementing onProvideAssistContent(), you provide the Assistant with structured
     * information about the current foreground content.
     *
     * This contextual information enables the Assistant to continue being helpful after the user
     * enters your app.
     */
    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)

        // JSON-LD object based on Schema.org structured data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // This is just an example, more accurate information should be provided
            outContent.structuredData = JSONObject()
                .put("@type", "ExerciseObservation")
                .put("name", "My last runs")
                .put("url", "https://fit-actions.firebaseapp.com/stats")
                .toString()
        }
    }

    /**
     * Callback method from the FitStatsFragment to indicate that the tracking activity flow
     * should be shown.
     */
    override fun onStartActivity() {
        updateView(
            newFragmentClass = FitTrackingFragment::class.java,
            arguments = Bundle().apply {
                putSerializable(FitTrackingFragment.PARAM_TYPE, FitActivity.Type.RUNNING)
            },
            toBackStack = true
        )
    }

    /**
     * Callback method when an activity stops.
     * We could show a details screen, for now just go back to home screen.
     */
    override fun onActivityStopped(activityId: String) {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            updateView(FitStatsFragment::class.java)
        }
    }

    /**
     * Handles the action from the intent base on the type.
     *
     * @receiver the intent to handle
     */
    private fun Intent.handleIntent() {
        when (action) {
            // When the action is triggered by a deep-link, Intent.Action_VIEW will be used
            Intent.ACTION_VIEW -> handleDeepLink(data)
            // Otherwise start the app as you would normally do.
            else -> showDefaultView()
        }
    }

    /**
     * Use the URI provided by the intent to handle the different deep-links
     */
    private fun handleDeepLink(data: Uri?) {
        // path is normally used to indicate which view should be displayed
        // i.e https://fit-actions.firebaseapp.com/start?exerciseType="Running" -> path = "start"
        var actionHandled = true
        when (data?.path) {
            DeepLink.STATS -> {
                updateView(FitStatsFragment::class.java)
            }
            DeepLink.START -> {
                // Get the parameter defined as "exerciseType" and add it to the fragment arguments
                val exerciseType = data.getQueryParameter(DeepLink.Params.ACTIVITY_TYPE).orEmpty()
                val type = FitActivity.Type.find(exerciseType)
                val arguments = Bundle().apply {
                    putSerializable(FitTrackingFragment.PARAM_TYPE, type)
                }

                updateView(FitTrackingFragment::class.java, arguments)
            }
            DeepLink.STOP -> {
                // Stop the tracking service if any and return to home screen.
                stopService(Intent(this, FitTrackingService::class.java))
                updateView(FitStatsFragment::class.java)
            }
            else -> {
                // path is not supported or invalid, start normal flow.
                showDefaultView()

                // Unknown or invalid action
                actionHandled = false
            }
        }

        notifyActionSuccess(actionHandled)
    }

    /**
     * Log a success or failure of the received action based on if your app could handle the action
     *
     * Required to help giving Assistant visibility over success or failure of an action sent to the app.
     * Otherwise, it can’t confidently send user’s to your app for fulfillment.
     */
    private fun notifyActionSuccess(succeed: Boolean) {
        @Suppress("ConstantConditionIf")
        if (!BuildConfig.FIREBASE_ENABLED) {
            return
        }

        intent.getStringExtra(DeepLink.Actions.ACTION_TOKEN_EXTRA)?.let { actionToken ->
            val actionStatus = if (succeed) {
                Action.Builder.STATUS_TYPE_COMPLETED
            } else {
                Action.Builder.STATUS_TYPE_FAILED
            }
            val action = AssistActionBuilder()
                .setActionToken(actionToken)
                .setActionStatus(actionStatus)
                .build()

            // Send the end action to the Firebase app indexing.
            FirebaseUserActions.getInstance().end(action)
        }
    }

    /**
     * Show ongoing activity or stats if none
     */
    private fun showDefaultView() {
        val onGoing = FitRepository.getInstance(this).getOnGoingActivity().value
        val fragmentClass = if (onGoing != null) {
            FitTrackingFragment::class.java
        } else {
            FitStatsFragment::class.java
        }
        updateView(fragmentClass)
    }

    /**
     * Utility method to update the Fragment with the given arguments.
     */
    private fun updateView(
        newFragmentClass: Class<out Fragment>,
        arguments: Bundle? = null,
        toBackStack: Boolean = false
    ) {
        val currentFragment = supportFragmentManager.fragments.firstOrNull()
        if (currentFragment != null && currentFragment::class.java == newFragmentClass) {
            return
        }

        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            newFragmentClass.classLoader!!,
            newFragmentClass.name
        )
        fragment.arguments = arguments

        supportFragmentManager.beginTransaction().run {
            replace(R.id.fitActivityContainer, fragment)
            if (toBackStack) {
                addToBackStack(null)
            }
            commit()
        }
    }
}
