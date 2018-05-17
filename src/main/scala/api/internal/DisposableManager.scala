package api.internal

import io.reactivex.disposables.{CompositeDisposable, Disposable}

object DisposableManager {

  private[this] var compositeDisposable: CompositeDisposable = new CompositeDisposable()

  private def getCompositeDisposable: CompositeDisposable = {
    if (compositeDisposable.isDisposed)
      compositeDisposable = new CompositeDisposable()
    compositeDisposable
  }

  def add(d: Disposable): Disposable = {
    getCompositeDisposable.add(d)
    d
  }

  def addAll(disposables: Seq[Disposable]): Seq[Disposable] = {
    getCompositeDisposable.addAll(disposables:_*)
    disposables
  }

  def disposeAll(): Unit = getCompositeDisposable.dispose()

}
