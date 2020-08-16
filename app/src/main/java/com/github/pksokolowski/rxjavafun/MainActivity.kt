package com.github.pksokolowski.rxjavafun

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.pksokolowski.rxjavafun.di.ViewModelFactory
import com.jakewharton.rxbinding4.view.clicks
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        output.movementMethod = ScrollingMovementMethod()

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)

        setupUiInteraction()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.getPosts().observe(this, Observer {
            writeln(it.map { "title = ${it.title}\nbody=${it.body}" })
        })
    }

    private fun setupUiInteraction() {
        startButton.setOnClickListener {
            viewModel.fetchPostsOfAllUsers()
        }

        output.clicks()
            .subscribe{
                output.text = "I've been clicked"
            }
    }


    @SuppressLint("SetTextI18n")
    private fun writeln(content: String) {
        if(content.isEmpty()) return
        output.append("\n$content")
        scroll()
    }

    private fun writeln(lines: List<String>){
        val builder = StringBuilder()
        lines.forEach { builder.appendln(it) }
        writeln(builder.toString())
    }

    private fun scroll() {
        val scrollAmount = output.layout.getLineTop(output.lineCount) - output.height
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            output.scrollTo(0, scrollAmount)
        else
            output.scrollTo(0, 0)
    }

    private fun appendOutputAndScrollABit(content: String) {
        var lcount = 0
        val index = content.indexOfFirst { c -> c == '\n' && lcount++ > 5 }
        val firstPart = if (index > -1) content.substring(0, index) else content
        output.append(firstPart)
        scroll()
        if (index > -1) {
            val secondPart = content.substring(index)
            output.append(secondPart)
        }
    }
}
