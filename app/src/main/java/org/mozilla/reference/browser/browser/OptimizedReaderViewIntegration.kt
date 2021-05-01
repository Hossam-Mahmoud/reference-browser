package org.mozilla.reference.browser.browser

import android.content.Context
import android.view.ViewStub
import androidx.compose.ui.platform.ComposeView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.readerview.ReaderViewFeature
import mozilla.components.feature.readerview.view.ReaderViewControlsView
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.reference.browser.R

class OptimizedReaderViewIntegration (
        val context: Context,
        val engine: Engine,
        val store: BrowserStore,
        val toolbar: BrowserToolbar,
        val viewStub: ViewStub,
        val readerButtonStub: ViewStub
) : LifecycleAwareFeature, UserInteractionHandler {

    private var readerViewButtonVisible = false
    private var view: ReaderViewControlsView? = null
    private var readerViewAppearanceButton: FloatingActionButton? = null
    private var readerViewButton: BrowserToolbar.ToggleButton? = null

    private val feature = ReaderViewFeature(context, engine, store, null ) { available, active ->
        if(available && view == null){
            view = viewStub.inflate() as ReaderViewControlsView
            readerViewAppearanceButton = readerButtonStub.inflate() as FloatingActionButton
            val layoutParams = readerViewAppearanceButton?.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.behavior = ReaderViewAppearanceButtonBehavior()
            readerViewAppearanceButton?.requestLayout()
            updateFeature()
            initReaderViewButton()
        }
        readerViewButtonVisible = available
        readerViewButton?.setSelected(active)

        if (active) readerViewAppearanceButton?.show() else readerViewAppearanceButton?.hide()
        toolbar.invalidateActions()
    }



    override fun start() {
        feature.start()
    }

    override fun stop() {
        feature.stop()
    }

    override fun onBackPressed(): Boolean {
        return feature.onBackPressed()
    }

    private fun updateFeature(){
        view?.let {
            feature.updateControlsView(it)
        }
    }

    private fun initReaderViewButton(){
        readerViewButton = BrowserToolbar.ToggleButton(
            image = ContextCompat.getDrawable(context, R.drawable.mozac_ic_reader_mode)!!,
            imageSelected = ContextCompat.getDrawable(context, R.drawable.mozac_ic_reader_mode)!!.mutate().apply {
                setTint(ContextCompat.getColor(context, R.color.photonBlue40))
            },
            contentDescription = "Enable Reader View",
            contentDescriptionSelected = "Disable Reader View",
            selected = store.state.selectedTab?.readerState?.active ?: false,
            visible = { readerViewButtonVisible }
        ) { enabled ->
            if (enabled) {
                feature.showReaderView()
                readerViewAppearanceButton?.show()
            } else {
                feature.hideReaderView()
                feature.hideControls()
                readerViewAppearanceButton?.hide()
            }
        }.also{
            toolbar.addPageAction(it)
            readerViewAppearanceButton?.setOnClickListener{ feature.showControls() }
        }
    }
}

