package com.skedgo.tripkit.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.skedgo.tripkit.ui.databinding.FragmentGenericNoteBinding
import kotlinx.coroutines.launch

data class GenericNote(
        val fragmentTitle: String? = "",
        val inputValue: String? = ""
)

class GenericNoteViewModel : ViewModel() {
    val fragmentTitle = ObservableField<String>()
    val inputValue = ObservableField<String>()
}

private const val ARG_TITLE = "arg_detail_title"
private const val ARG_INPUT_VALUE = "arg_input_value"

class GenericNoteDialogFragment : Fragment() {

    lateinit var viewModel: GenericNoteViewModel
    private var argTitle: String = ""
    private var argInputValue: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get("GenericNoteViewModel", GenericNoteViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding = FragmentGenericNoteBinding.inflate(inflater)
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            argTitle = arguments?.getString(ARG_TITLE, "") ?: ""
            argInputValue = arguments?.getString(ARG_INPUT_VALUE, "") ?: ""

            setLabels()
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = argTitle
    }

    private fun setLabels() {
        viewModel.fragmentTitle.set(argTitle)
    }

    companion object {
        @JvmStatic
        fun newInstance(fragmentTitle: String = "", inputValue: String = "") = GenericNoteDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, fragmentTitle)
                putString(ARG_INPUT_VALUE, inputValue)
            }
        }
    }
}