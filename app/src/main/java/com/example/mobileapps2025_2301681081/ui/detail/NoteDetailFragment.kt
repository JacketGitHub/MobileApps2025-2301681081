package com.example.mobileapps2025_2301681081.ui.detail

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.mobileapps2025_2301681081.R
import com.example.mobileapps2025_2301681081.data.AppDatabase
import com.example.mobileapps2025_2301681081.data.NoteRepository
import com.example.mobileapps2025_2301681081.databinding.FragmentNoteDetailBinding
import com.example.mobileapps2025_2301681081.ui.NoteViewModel
import com.example.mobileapps2025_2301681081.ui.NoteViewModelFactory
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch

class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    private val args: NoteDetailFragmentArgs by navArgs()

    private val viewModel: NoteViewModel by activityViewModels {
        val dao = AppDatabase.getInstance(requireContext()).noteDao()
        NoteViewModelFactory(NoteRepository(dao))
    }

    // Whether we are editing an existing note (true) or creating a new one (false)
    private val isEditMode get() = args.noteId != 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupSaveButton()
        setupQrButton()

        if (isEditMode) {
            viewModel.loadNote(args.noteId)
            observeCurrentNote()
        } else {
            viewModel.clearCurrentNote()
        }

        observeSaveResult()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_detail, menu)
                    // Only show delete option when editing an existing note
                    menu.findItem(R.id.action_delete)?.isVisible = isEditMode
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_delete -> {
                            confirmAndDeleteNote()
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
    }

    private fun observeCurrentNote() {
        viewModel.currentNote.observe(viewLifecycleOwner) { note ->
            note?.let {
                binding.editTitle.setText(it.title)
                binding.editBody.setText(it.body)
            }
        }
    }

    private fun setupSaveButton() {
        binding.buttonSave.setOnClickListener {
            val title = binding.editTitle.text.toString()
            val body = binding.editBody.text.toString()
            val existingId = if (isEditMode) args.noteId else null
            viewModel.saveNote(title, body, existingId)
        }
    }

    private fun setupQrButton() {
        binding.buttonQr.setOnClickListener {
            val title = binding.editTitle.text.toString().trim()
            val body = binding.editBody.text.toString().trim()

            if (title.isEmpty() && body.isEmpty()) {
                Toast.makeText(requireContext(), R.string.qr_empty_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val qrContent = buildString {
                if (title.isNotEmpty()) append("Title: $title\n\n")
                if (body.isNotEmpty()) append(body)
            }

            showQrDialog(qrContent)
        }
    }

    private fun showQrDialog(content: String) {
        try {
            val encoder = BarcodeEncoder()
            val hints = mapOf(com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8")
            val bitmap: Bitmap = encoder.encodeBitmap(
                content,
                BarcodeFormat.QR_CODE,
                600,
                600,
                hints
            )

            val imageView = ImageView(requireContext()).apply {
                setImageBitmap(bitmap)
                val padding = 48
                setPadding(padding, padding, padding, padding)
            }

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.qr_dialog_title)
                .setView(imageView)
                .setPositiveButton(R.string.close, null)
                .show()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.qr_generate_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmAndDeleteNote() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_message)
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                viewModel.currentNote.value?.let { note ->
                    viewModel.deleteNote(note)
                    findNavController().popBackStack()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeSaveResult() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveResult.collect { success ->
                    if (success == true) {
                        findNavController().popBackStack()
                    } else {
                        binding.editTitle.error = getString(R.string.title_required)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
