package com.example.unimarket.view.profile.manage

import kotlin.io.root
import kotlin.text.orEmpty
import kotlin.toString

class `BusinessInfoEditorDialog.kt` : com.google.android.material.bottomsheet.BottomSheetDialogFragment() {

    private var _binding: DialogBusinessInfoEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BusinessManageViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: android.os.Bundle?): android.app.Dialog {
        return _root_ide_package_.com.google.android.material.bottomsheet.BottomSheetDialog(
            _root_ide_package_.androidx.fragment.app.Fragment.requireContext(),
            _root_ide_package_.androidx.fragment.app.DialogFragment.getTheme
        )
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): android.view.View {
        _binding = DialogBusinessInfoEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = _root_ide_package_.androidx.fragment.app.Fragment.requireArguments()
        binding.inputBusinessName.setText(args.getString(_root_ide_package_.com.example.unimarket.view.profile.manage.`BusinessInfoEditorDialog.kt`.Companion.ARG_NAME).orEmpty())
        binding.inputBusinessDescription.setText(args.getString(_root_ide_package_.com.example.unimarket.view.profile.manage.`BusinessInfoEditorDialog.kt`.Companion.ARG_DESCRIPTION).orEmpty())
        binding.inputBusinessLogo.setText(args.getString(_root_ide_package_.com.example.unimarket.view.profile.manage.`BusinessInfoEditorDialog.kt`.Companion.ARG_LOGO).orEmpty())
        binding.inputBusinessBanner.setText(args.getString(_root_ide_package_.com.example.unimarket.view.profile.manage.`BusinessInfoEditorDialog.kt`.Companion.ARG_BANNER).orEmpty())
        binding.inputBusinessStatus.setText(args.getString(_root_ide_package_.com.example.unimarket.view.profile.manage.`BusinessInfoEditorDialog.kt`.Companion.ARG_STATUS).orEmpty(), false)
        binding.switchBusinessOpen.isChecked = args.getBoolean(_root_ide_package_.com.example.unimarket.view.profile.manage.`BusinessInfoEditorDialog.kt`.Companion.ARG_IS_OPEN, true)

        val statusOptions = _root_ide_package_.kotlin.collections.listOf(
            _root_ide_package_.androidx.fragment.app.Fragment.getString(_root_ide_package_.com.example.unimarket.R.string.business_owner_status_open),
            _root_ide_package_.androidx.fragment.app.Fragment.getString(_root_ide_package_.com.example.unimarket.R.string.business_owner_status_closed)
        )
        val statusAdapter = _root_ide_package_.android.widget.ArrayAdapter(
            _root_ide_package_.androidx.fragment.app.Fragment.requireContext(),
            _root_ide_package_.android.R.layout.simple_list_item_1,
            statusOptions
        )
        binding.inputBusinessStatus.setAdapter(statusAdapter)

        binding.btnSaveBusiness.setOnClickListener {
            val input =
                _root_ide_package_.com.example.unimarket.view.profile.manage.BusinessInfoInput(
                    businessId = args.getString(_root_ide_package_.com.example.unimarket.view.profile.manage.`BusinessInfoEditorDialog.kt`.Companion.ARG_BUSINESS_ID)
                        .orEmpty(),
                    name = binding.inputBusinessName.text?.toString().orEmpty(),
                    description = binding.inputBusinessDescription.text?.toString().orEmpty(),
                    status = binding.inputBusinessStatus.text?.toString().orEmpty(),
                    isOpen = binding.switchBusinessOpen.isChecked,
                    logoUrl = binding.inputBusinessLogo.text?.toString().orEmpty(),
                    bannerUrl = binding.inputBusinessBanner.text?.toString().orEmpty()
                )
            viewModel.saveBusinessInfo(input)
            _root_ide_package_.com.google.android.material.bottomsheet.BottomSheetDialogFragment.dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_BUSINESS_ID = "arg_business_id"
        private const val ARG_NAME = "arg_name"
        private const val ARG_DESCRIPTION = "arg_description"
        private const val ARG_STATUS = "arg_status"
        private const val ARG_IS_OPEN = "arg_is_open"
        private const val ARG_LOGO = "arg_logo"
        private const val ARG_BANNER = "arg_banner"

        fun newInstance(data: com.example.unimarket.view.profile.manage.BusinessInfoEditorData): com.example.unimarket.view.profile.manage.`BusinessInfoEditorDialog.kt` {
            val fragment =
                _root_ide_package_.com.example.unimarket.view.profile.manage.`BusinessInfoEditorDialog.kt`()
            val bundle = _root_ide_package_.android.os.Bundle()
            bundle.putString(ARG_BUSINESS_ID, data.businessId)
            bundle.putString(ARG_NAME, data.name)
            bundle.putString(ARG_DESCRIPTION, data.description)
            bundle.putString(ARG_STATUS, data.status)
            bundle.putBoolean(ARG_IS_OPEN, data.isOpen)
            bundle.putString(ARG_LOGO, data.logoUrl)
            bundle.putString(ARG_BANNER, data.bannerUrl)
            fragment.arguments = bundle
            return fragment
        }
    }
}