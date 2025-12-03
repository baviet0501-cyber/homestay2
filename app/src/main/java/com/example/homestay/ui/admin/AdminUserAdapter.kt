package com.example.homestay.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.R
import com.example.homestay.data.api.models.AdminUserData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminUserAdapter(
    private val users: MutableList<AdminUserData>,
    private val onDeleteClick: (AdminUserData) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<AdminUserData>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    fun removeUser(userId: String) {
        val index = users.indexOfFirst { it.id == userId }
        if (index != -1) {
            users.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAvatar: TextView = itemView.findViewById(R.id.tv_avatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tv_user_email)
        private val tvUserPhone: TextView = itemView.findViewById(R.id.tv_user_phone)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(user: AdminUserData) {
            // Avatar: Lấy chữ cái đầu
            val firstChar = user.fullName.firstOrNull()?.uppercaseChar() ?: 'U'
            tvAvatar.text = firstChar.toString()
            
            tvUserName.text = user.fullName
            tvUserEmail.text = user.email
            tvUserPhone.text = user.phone

            btnDelete.setOnClickListener {
                onDeleteClick(user)
            }
        }
    }
}

