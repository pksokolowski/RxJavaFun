package com.github.pksokolowski.rxjavafun

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.pksokolowski.rxjavafun.di.ViewModelFactory
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    var postsAdapter: PostsAdapter = PostsAdapter()

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)

        recyclerView.adapter = postsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupUiInteraction()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.getPosts().observe(this, Observer {
            postsAdapter.setItems(it)
        })
    }

    private fun setupUiInteraction() {
        startButton.setOnClickListener {
            viewModel.fetchPosts()
        }
    }

}
