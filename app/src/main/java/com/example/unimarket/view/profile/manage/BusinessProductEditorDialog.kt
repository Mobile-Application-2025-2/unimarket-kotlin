package com.example.unimarket.view.profile.manage

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.example.unimarket.R
import com.example.unimarket.databinding.DialogBusinessProductEditorBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale
import coil.load

class BusinessProductEditorDialog : BottomSheetDialogFragment() {

    private var _binding: DialogBusinessProductEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BusinessManageViewModel by activityViewModels()
    private var categoryMap: Map<String, String> = emptyMap()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBusinessProductEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val isEdit = args.getBoolean(ARG_IS_EDIT, false)
        binding.tvTitle.text = getString(
            if (isEdit) R.string.business_owner_product_edit_title else R.string.business_owner_product_add_title
        )

        val categoryLabels = args.getStringArrayList(ARG_CATEGORY_LABELS) ?: arrayListOf()
        val categoryIds = args.getStringArrayList(ARG_CATEGORY_IDS) ?: arrayListOf()
        categoryMap = categoryLabels.mapIndexed { index, label ->
            label to categoryIds.getOrNull(index).orEmpty()
        }.toMap()
        val originalCategoryId = args.getString(ARG_CATEGORY_ID).orEmpty()
        val originalLabel = args.getString(ARG_CATEGORY_LABEL).orEmpty()
        if (originalLabel.isNotBlank() && originalLabel !in categoryMap.keys) {
            categoryMap = categoryMap + (originalLabel to originalCategoryId)
        }

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            categoryLabels
        )
        binding.inputCategory.setAdapter(categoryAdapter)

        binding.inputName.setText(args.getString(ARG_NAME).orEmpty())
        binding.inputDescription.setText(args.getString(ARG_DESCRIPTION).orEmpty())
        binding.inputImage.setText(args.getString(ARG_IMAGE).orEmpty())
        binding.inputCategory.setText(originalLabel, false)
        val priceValue = args.getDouble(ARG_PRICE, 0.0)
        if (priceValue > 0.0) {
            binding.inputPrice.setText(formatPrice(priceValue))
        }

        updatePreview(args.getString(ARG_IMAGE).orEmpty())
        binding.inputImage.doAfterTextChanged { updatePreview(it?.toString().orEmpty()) }

        binding.btnSave.setOnClickListener {
            val input = ProductEditorInput(
                businessId = args.getString(ARG_BUSINESS_ID).orEmpty(),
                productId = args.getString(ARG_PRODUCT_ID),
                name = binding.inputName.text?.toString().orEmpty(),
                description = binding.inputDescription.text?.toString().orEmpty(),
                imageUrl = binding.inputImage.text?.toString().orEmpty(),
                priceText = binding.inputPrice.text?.toString().orEmpty(),
                categoryLabel = binding.inputCategory.text?.toString().orEmpty(),
                categoryId = categoryMap[binding.inputCategory.text?.toString().orEmpty()]
            )
            viewModel.saveProduct(input)
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updatePreview(url: String) {
        binding.imgPreview.load(url.ifBlank { null }) {
            crossfade(true)
            placeholder(R.drawable.formas)
            error(R.drawable.formas)
        }
    }

    private fun formatPrice(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(Locale.getDefault(), "%.0f", value)
        } else {
            String.format(Locale.getDefault(), "%.2f", value)
        }
    }

    companion object {
        private const val ARG_BUSINESS_ID = "arg_business_id"
        private const val ARG_PRODUCT_ID = "arg_product_id"
        private const val ARG_NAME = "arg_name"
        private const val ARG_DESCRIPTION = "arg_description"
        private const val ARG_IMAGE = "arg_image"
        private const val ARG_PRICE = "arg_price"
        private const val ARG_CATEGORY_ID = "arg_category_id"
        private const val ARG_CATEGORY_LABEL = "arg_category_label"
        private const val ARG_CATEGORY_IDS = "arg_category_ids"
        private const val ARG_CATEGORY_LABELS = "arg_category_labels"
        private const val ARG_IS_EDIT = "arg_is_edit"

        fun newInstance(
            data: ProductEditorData,
            categoryOptions: List<CategoryOption>
        ): BusinessProductEditorDialog {
            val fragment = BusinessProductEditorDialog()
            val bundle = Bundle()
            bundle.putString(ARG_BUSINESS_ID, data.businessId)
            bundle.putString(ARG_PRODUCT_ID, data.productId)
            bundle.putString(ARG_NAME, data.name)
            bundle.putString(ARG_DESCRIPTION, data.description)
            bundle.putString(ARG_IMAGE, data.imageUrl)
            bundle.putDouble(ARG_PRICE, data.price)
            bundle.putString(ARG_CATEGORY_ID, data.categoryId)
            bundle.putString(ARG_CATEGORY_LABEL, data.categoryLabel)
            bundle.putBoolean(ARG_IS_EDIT, !data.productId.isNullOrBlank())
            bundle.putStringArrayList(ARG_CATEGORY_IDS, ArrayList(categoryOptions.map { it.id }))
            bundle.putStringArrayList(ARG_CATEGORY_LABELS, ArrayList(categoryOptions.map { it.label }))
            fragment.arguments = bundle
            return fragment
        }
    }
}