package com.skedgo.tripkit.ui.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.gson.Gson
import com.skedgo.tripkit.ui.dialog.GenericLoadingDialog

/**
 * To act as a super class for all other activities.
 * Passing ViewDataBinding to include initialization which is common for all activities
 * that's using databinding
 */
abstract class BaseActivity<V : ViewDataBinding> : AppCompatActivity() {

    protected val gson = Gson()

    @get:LayoutRes
    protected abstract val layoutRes: Int

    protected lateinit var binding: V

    private val loadingDialog: GenericLoadingDialog by lazy(mode = LazyThreadSafetyMode.NONE) {
        GenericLoadingDialog(this@BaseActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, layoutRes)

        onCreated(savedInstanceState)
    }

    protected abstract fun onCreated(instance: Bundle?)

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    protected fun showLoading(isLoading: Boolean) {
        loadingDialog.let {
            if (isLoading && !loadingDialog.isShowing)
                loadingDialog.show()
            else if (!isLoading && loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }
        }
    }

    override fun onDestroy() {
        showLoading(false)
        super.onDestroy()
    }

    //For simple yet dynamic control (basic control) of toolbar
    fun setToolbar(
        show: Boolean = false,
        showBackButton: Boolean = false,
        showCustom: Boolean = false,
        title: String = "",
        layout: Int = -1,
        listener: View.OnClickListener? = null
    ) {
        val actionBar = supportActionBar

        actionBar?.run {
            if (show) {
                show()
                displayOptions = ActionBar.DISPLAY_SHOW_TITLE

                setHomeButtonEnabled(showBackButton)
                setDisplayHomeAsUpEnabled(showBackButton)

                if (title != "") {
                    setDisplayShowTitleEnabled(true)
                    this@run.title = title
                } else
                    setDisplayShowTitleEnabled(false)

                if (showCustom) {
                    setDisplayShowCustomEnabled(layout > 0)
                    val view =
                        (getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                            layout,
                            null
                        )
                    customView = view
                }
            } else
                hide()
        }
    }
}