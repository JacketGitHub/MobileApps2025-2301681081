package com.example.mobileapps2025_2301681081.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapps2025_2301681081.data.AppDatabase
import com.example.mobileapps2025_2301681081.data.NoteRepository
import com.example.mobileapps2025_2301681081.databinding.FragmentNoteListBinding
import com.example.mobileapps2025_2301681081.ui.NoteViewModel
import com.example.mobileapps2025_2301681081.ui.NoteViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

class NoteListFragment : Fragment() {
    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteViewModel by activityViewModels {
        val dao = AppDatabase.getInstance(requireContext()).noteDao()
        NoteViewModelFactory(NoteRepository(dao))
    }

    private lateinit var adapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        observeNotes()
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter { note ->
            val action = NoteListFragmentDirections
                .actionNoteListFragmentToNoteDetailFragment(noteId = note.id)
            findNavController().navigate(action)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@NoteListFragment.adapter
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                r: RecyclerView,
                v: RecyclerView.ViewHolder,
                t: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val note = adapter.currentList[viewHolder.adapterPosition]
                viewModel.deleteNote(note)
                Snackbar.make(binding.root, "Note deleted", Snackbar.LENGTH_SHORT).show()
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = android.graphics.Paint()

                paint.color = "#F44336".toColorInt()

                if (dX > 0) {
                    c.drawRect(
                        itemView.left.toFloat(),
                        itemView.top.toFloat(),
                        itemView.left + dX,
                        itemView.bottom.toFloat(),
                        paint
                    )
                } else {
                    c.drawRect(
                        itemView.right + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat(),
                        paint
                    )
                }

                val icon = androidx.core.content.ContextCompat.getDrawable(
                    recyclerView.context,
                    android.R.drawable.ic_menu_delete
                )!!
                icon.setTint(android.graphics.Color.WHITE)

                val iconMargin = 48
                val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                val iconBottom = iconTop + icon.intrinsicHeight

                if (dX > 0) {
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = iconLeft + icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                } else {
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = iconRight - icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                }

                icon.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(binding.recyclerView)
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            val action = NoteListFragmentDirections
                .actionNoteListFragmentToNoteDetailFragment(noteId = 0)
            findNavController().navigate(action)
        }
    }

    private fun observeNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allNotes.collectLatest { notes ->
                    adapter.submitList(notes)
                    binding.textEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
