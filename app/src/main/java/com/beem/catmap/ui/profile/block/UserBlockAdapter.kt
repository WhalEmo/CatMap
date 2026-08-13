package com.beem.catmap.ui.profile.block

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.R
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Objects
class UserBlockAdapter(
    private val onBlockClick: (UserModel) -> Unit,
    private val onUserClick: (String?) -> Unit
) : ListAdapter<UserModel, UserBlockAdapter.ViewHolder>(DiffCallback()) {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.herbi_engellenen, parent, false)


        return ViewHolder(view)
    }


    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position),onUserClick)
    }

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val photo = itemView.findViewById<CircleImageView>(R.id.recyclerFotoImageView)
        private val username = itemView.findViewById<TextView>(R.id.RecyclerkullaniciAdi)
        private val blockButton = itemView.findViewById<Button>(R.id.engel)
        fun bind(userModel: UserModel, onUserClick: (String?) -> Unit) {
            username.text = userModel.username


            Glide.with(itemView)
                .load(userModel.photoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .centerCrop()
                .into(photo)



            blockButton.setOnClickListener {
                onBlockClick(userModel)
            }
            username.setOnClickListener {
                onUserClick(userModel.id)
            }
        }
    }



    class DiffCallback :
        DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.id == newItem.id
        }


        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return Objects.equals(oldItem, newItem);
        }
    }
}