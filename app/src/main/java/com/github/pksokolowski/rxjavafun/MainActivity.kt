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
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var viewModel: MainViewModel

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        output.movementMethod = ScrollingMovementMethod()

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)

        setupUiInteraction()
        setupObservers()
        setupOutputObservers()

        inputEditText.requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun setupOutputObservers() {
        viewModel.output
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { writeln(it) }
            .addTo(disposables)

        viewModel.outputUpdateLastLine
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { replaceLastLineWith(it) }
            .addTo(disposables)
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
            .addTo(disposables)

        inputEditText.textChanges()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.toString() }
            .subscribe(::handleCommand)
            .addTo(disposables)

        searchEditText.textChanges()
            .skipInitialValue()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.toString() }
            .flatMapSingle { viewModel.findVocabulary(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { vocabulary ->
                output.text = vocabulary
                    .joinToString(separator = "\n")
            }
            .addTo(disposables)
    }

    private fun displayString(@StringRes content: Int) = displayString(getString(content))

    private fun displayString(content: String) {
        output.text = content
    }

    private fun replaceLastLineWith(content: String) {
        val current = output.text.toString()
        val lastLineIndex = current.indexOfLast { it == '\n' }.let { index ->
            if (index == -1) 0 else index + 1
        }

        output.text = current.substring(0, lastLineIndex) + content
    }

    private val commands = hashMapOf(
        "maybe" to {
            viewModel.maybeFun()
                .onErrorComplete { displayString(getString(R.string.error_maybe)); true }
                .subscribe(::displayString)
                .addTo(disposables)
        },
        "backpressure-unhandled" to {
            viewModel.backPressureUnhandled()
        },
        "backpressure-sample" to {
            viewModel.backPressureSample()
        },
        "timer" to {
            viewModel.getTimer()
        },
        "combine-latest" to {
            viewModel.combineLatest()
        }
    )

    /**
     * Simplifies UI a bit by taking care of commands.
     */
    private fun handleCommand(input: String) {
        viewModel.clearOngoingSampleStreams()
        displayString("")

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
