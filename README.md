# Приложение за правене на бележки

## Изготвен от Мирослав Люгов, 2301681081

## Prerequisites
+ Android Studio Ladybug or newer, JDK 17+
+ Needs an emulator or device with API 24+.

Проектът е, най-общо казано, свързан с правене и запазване на бележки(Notes),  прецених да направя след като забелязах, че в моя телефон няма всъщност приложение за бележки, което ми се стори странно. Замислих се каква друга функционалност може да има такова приложение, която да е по-чудновата, и затова добавих и QR код функционалност.

### 1. Ключови функции
+ CRUD операции - създаване, четене, редактиране и изтриване са включени.
+ QR code - генериране на QR кодове за споделяне на бележки
+ Persistent storage - бележките не се трият при затваряне на приложението. Запазват се в паметта
+ Динамично сортиране - бележките автоматично се сортират от най-нови към най-стари.

### 2. Технически стек (Technical stack)
+ Език - Kotlin
+ Архитектура - MVVM (Model-View-ViewModel) с Repository pattern
+ База Данни - Room[1]
+ Навигация - Jetpack Navigation Component
+ Паралелност/Concurrency - Kotlin Coroutines and Flow[1]
+ Компоненти на UI -
  - Material Design 3
  - ViewBinding
  - RecyclerView за представяне на бележките.
+ Външни библиотеки -  
  - ZXing[2] - за генериране на QR кодове
  - Lifecycle[3] - за Lifecycle-aware компоненти

### 3. Структура на проекта
+ data/ - там са Room entity-то (Note), DAO (Data Access Object, NoteDao), дефиницията за базата данни и NoteRepository-то.
+ ui/detail - тук е логиката за гледане, редактиране и генериране на QR кодове за бележки.
+ ui/list - Fragments и Adapters за главния списък с бележки.
+ ui/ - ViewModel логика и factories използвани из цялото приложение.

### 4. Schema за базата данни
В Note се съдържа информацията за базата данни, която се състой от 4 стойности - 
+ id - Главен ключ, автогенериращ
+ title - низ, задължителен
+ body - низ, незадължителен
+ timestamp - Long, използван за сортиране, използва текущото време в системата до милисекундата (System.currentTimeMillis())

### 5. Подробности за имплементация
ViewModel Sharing pattern - вместо всеки Fragment да си има свои данни, те ползват една единствена инстанция на NoteViewModel, която “живее” докато MainActivity е още активно. Имплементирано е, като NoteListFragment и NoteDetailFragment извикват activityViewModels().

```
private val viewModel: NoteViewModel by activityViewModels {
        val dao = AppDatabase.getInstance(requireContext()).noteDao()
        NoteViewModelFactory(NoteRepository(dao))
    }
```

Така се постига и лекота на паметта и скорост, защото като се изтрие бележка или се направи нова няма да има нужда ръчно да се предават бележките между fragments чрез аргументи.

+ Потокът от данни
  - Приложението Unidirectional Data Flow (UDF), тоест данни могат да вървят от UI до ViewModel и обратно.
```
fun saveNote(title: String, body: String, existingId: Int? = null) {
        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()

        if (trimmedTitle.isEmpty()) {
            _saveResult.value = false
            return
        }

        viewModelScope.launch {
            try {
                if (existingId != null && existingId != 0) {
                    val updated = Note(id = existingId, title = trimmedTitle, body = trimmedBody)
                    repository.update(updated)
                } else {
                    val newNote = Note(title = trimmedTitle, body = trimmedBody)
                    repository.insert(newNote)
                }
                _saveResult.value = true
            } catch (_: Exception) {
                _saveResult.value = false
            }
        }
    }
```

saveNote функцията гледа дали заглавието е празно, след което гледа дали id- то на бележката съществува или не, Ако да - се третира, че ще се прави редакция на бележката и само се вика update, ако не - се прави нова бележка. След saveNote има функция onResultHandled, която след връщане в MainActivity задава _saveResult.value = null, защото стойността остава true и после не може да се направи нова бележка, защо програма приема, че всичко е правилно и true и директно връща в MainActivity. Също така, всеки път като се влезе във View-то за правене на бележки се извиква resetState, за да изчисти полетата за заглавие и тяло на бележката.

+ Функции на DetailFragment и ListFragment
  - observeSaveResult() (DetailFragment)
      ```
      private fun observeSaveResult() {
              viewLifecycleOwner.lifecycleScope.launch {
                  viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                      viewModel.saveResult.collect { success ->
                          when (success) {
                              true -> {
                                  viewModel.onSaveResultHandled()
                                  findNavController().popBackStack()
                              }
                              false -> {
                                  binding.editTitle.error = getString(R.string.title_required)
                                  viewModel.onSaveResultHandled()
                              }
                              null -> { }
                          }
                      }
                  }
              }
          }
      ```

Използван е lifecycleScope.launch да се слуша за промени в състоянието на ViewModel, ако види true се връща назад в началния екран, ако false дава грешка за празно поле за заглавие.
  - observeNotes() (ListFragment)
    ```
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
    ```
Тази функция наблюдава и събира данните в БД-то и защото използва collectLatest списъка се обновява в мига в който има някаква промяна.

+ QR кодова генерация
  - QR кодове се генерират чрез ZXing библиотека и тя трансформира заглавието и тялото на бележката в един низ.
```
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
```
След това се извиква функцията за показване на кода след като данните на бележката се енкодирани в 600х600 битмап. Кода се показва на отделен UI елемент и самият код не се запазва в галерията на телефона, кода е активен само когато този отделен UI елемент е показан.

```
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
```

# Заключение
Има направени Unit тестове и Espresso тест направен, двата вида успешни. Има възможност за разширяване на приложението по няколко начина. Дали да се добави възможност за влагане на снимки в бележки, категоризиране, възможност за markdown support или дори възможност за заключване на някои бележки с биометрия. Единственият проблем до момента, който е малко по-коварен за оправяне е разликата между различните инструменти за сканиране на QR кодове, защото докато тествах функцията за дали QR кода кодира информацията правилно, някои от скенерите също така показваха интервали и новите абзаци със техните URL кодиране (%20 за интервал, например). Освен този проблем, не мисля, че има много други пропуски.
