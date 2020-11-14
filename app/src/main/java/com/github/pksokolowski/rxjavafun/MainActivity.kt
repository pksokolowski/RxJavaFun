package com.github.pksokolowski.rxjavafun

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.pksokolowski.rxjavafun.di.ViewModelFactory
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.widget.textChanges
import dagger.android.AndroidInjection
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
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
        startButton.clicks()
            .throttleFirst(4, TimeUnit.SECONDS)
            .subscribe { viewModel.fetchPostsOfAllUsers() }

        // a slight abomination, done for practice though
        clockButton.clicks().subscribe { viewModel.getTimer(output) }

        output.clicks()
            .subscribe {
                output.text = "I've been clicked"
            }

        inputEditText.textChanges()
            .debounce(1, TimeUnit.SECONDS)
            .map { it.toString() }
            .subscribe(::handleCommand)

        searchEditText.textChanges()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.toString() }
            .flatMapSingle { viewModel.findVocabulary(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { vocabulary ->
                output.text = vocabulary
                    .joinToString(separator = "\n")
            }
    }

    fun displayString(@StringRes content: Int) = displayString(getString(content))

    fun displayString(content: String) {
        output.text = content
    }

    private val commands = hashMapOf(
        "maybe" to {
            viewModel.maybeFun()
                .onErrorComplete { displayString(getString(R.string.error_maybe)); true }
                .subscribe(::displayString)
        }
    )

    /**
     * Simplifies UI a bit by taking care of commands.
     */
    private fun handleCommand(input: String) {
        val command = commands[input]
        if (command == null) {
            displayString(
                getString(
                    R.string.unknown_command,
                    commands.keys.toList().joinToString("\n")
                )
            )
            return
        }

        command()
    }

    @SuppressLint("SetTextI18n")
    private fun writeln(content: String) {
        if (content.isEmpty()) return
        output.append("\n$content")
        scroll()
    }

    private fun writeln(lines: List<String>) {
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
