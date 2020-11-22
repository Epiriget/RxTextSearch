package com.example.rxtextsearch

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import android.widget.TextView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.ReplaySubject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    lateinit var input: SearchView
    lateinit var counter: TextView
    lateinit var liveCounterView: TextView

    private lateinit var text: String
    private lateinit var textSplit: List<String>
    private val disposables = CompositeDisposable()
    private var liveCounter: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        counter = findViewById(R.id.counter)
        liveCounterView = findViewById(R.id.live_counter)
        input = findViewById(R.id.searchView)
        text = getString(R.string.text).toLowerCase()
        textSplit = text.split(".")


        val inputObservable = getInputObservable()
        val disposable = inputObservable
            .debounce(700, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .map { getOccurrencesCount(text, it.toLowerCase()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    counter.text = getString(R.string.formatted_counter, it)
                },
                {
                    counter.text = it.stackTrace.toString()
                }
            )

        val liveDisposable = inputObservable
            .debounce(700, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .flatMap {liveCounter = 0; getLiveObservable(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    liveCounter += it
                    liveCounterView.text = getString(R.string.formatted_live_counter, liveCounter)
                },
                {
                    Log.d("LiveObservable", it.message.toString())
                })

        disposables.addAll(liveDisposable, disposable)
    }


    private fun getInputObservable(): Observable<String> {
        val source: ReplaySubject<String> = ReplaySubject.create()

        input.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                source.onNext(newText)
                return true
            }
        })
        return source
    }

    private fun getLiveObservable(substring: String): Observable<Int> {
        return Observable.create { emitter ->
            textSplit.forEach { emitter.onNext(getOccurrencesCount(it, substring)) }
        }
    }


    private fun getOccurrencesCount(source: String, substring: String): Int {
        return if (substring.isNotEmpty())
            substring.toRegex().findAll(source).sumBy { 1 }
        else 0

    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }
}
