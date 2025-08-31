// Define o pacote ao qual esta classe pertence.
package com.example.appfirestore

// Importações de classes necessárias do AndroidX Lifecycle e Firebase.
import androidx.lifecycle.LiveData // Usado para observar dados que podem mudar.
import androidx.lifecycle.MutableLiveData // LiveData que pode ser modificado.
import androidx.lifecycle.ViewModel // Classe base para ViewModels, que sobrevivem a mudanças de configuração.
import com.example.appfirestore.ToDoTasks.Task // Importa a classe de modelo Task (provavelmente definida em outro lugar).
import com.google.firebase.Firebase // Ponto de entrada para o SDK do Firebase.
import com.google.firebase.auth.FirebaseAuth // Para gerenciamento de autenticação de usuários (login, cadastro, etc.).
import com.google.firebase.firestore.firestore // Para interagir com o banco de dados Cloud Firestore.

/**
 * AuthViewModel é responsável por gerenciar o estado de autenticação do usuário
 * e fornecer funcionalidades relacionadas à autenticação, como login, cadastro e logout.
 * Ele também contém uma classe interna TaskViewModel para gerenciar tarefas do usuário no Firestore.
 */
class AuthViewModel : ViewModel() { // Herda de ViewModel para reter o estado durante mudanças de configuração.

    // Instância privada do FirebaseAuth para interagir com os serviços de autenticação do Firebase.
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // MutableLiveData privado para manter o estado atual da autenticação.
    // Apenas o ViewModel pode modificar este valor.
    private val _authState = MutableLiveData<AuthState>()
    // LiveData público e imutável exposto para a UI observar as mudanças no estado de autenticação.
    val authState: LiveData<AuthState> = _authState

    // MutableLiveData privado para manter o email do usuário logado.
    private val _userEmail = MutableLiveData<String>()
    // LiveData público e imutável para a UI observar o email do usuário.
    val userEmail: LiveData<String> = _userEmail

    // Bloco de inicialização que é executado quando uma instância do AuthViewModel é criada.
    init {
        refreshUser() // Chama a função para verificar o estado atual do usuário.
    }

    /**
     * TaskViewModel é uma classe interna (e também um ViewModel) responsável por
     * gerenciar as operações CRUD (Criar, Ler, Atualizar, Deletar) para as tarefas (Tasks)
     * de um usuário específico no Cloud Firestore.
     *
     * Nota: Ter um ViewModel dentro de outro ViewModel é um padrão incomum.
     * Geralmente, ViewModels são independentes e podem ser compartilhados entre diferentes
     * componentes da UI se necessário, ou cada tela/componente tem seu próprio ViewModel.
     * Se TaskViewModel é específico para o AuthViewModel, poderia ser uma classe normal
     * injetada ou criada pelo AuthViewModel, não necessariamente outro ViewModel.
     * Se TaskViewModel é usado por outras partes da UI independentemente do estado de auth,
     * deveria ser uma classe ViewModel de nível superior separada.
     */
    class TaskViewModel : ViewModel() {
        // Instância do Cloud Firestore.
        private val db = Firebase.firestore
        // Instância do FirebaseAuth para obter o ID do usuário atual.
        private val auth = FirebaseAuth.getInstance()

        // MutableLiveData privado para a lista de tarefas.
        private val _tasks = MutableLiveData<List<Task>>()
        // LiveData público para a UI observar a lista de tarefas.
        val tasks: LiveData<List<Task>> = _tasks

        /**
         * Carrega as tarefas do usuário logado do Firestore em tempo real.
         * Adiciona um SnapshotListener para ouvir mudanças na coleção de tarefas.
         */
        fun loadTasks() {
            // Obtém o UID (ID único) do usuário atualmente logado. Se não houver usuário, retorna.
            val uid = auth.currentUser?.uid ?: return
            // Acessa a subcoleção 'tasks' dentro do documento do usuário na coleção 'users'.
            db.collection("users").document(uid).collection("tasks")
                // Adiciona um listener que será notificado sobre mudanças nos dados.
                .addSnapshotListener { snapshot, e ->
                    // Se houver um erro, retorna (idealmente, trataria o erro).
                    if (e != null) return@addSnapshotListener
                    // Se o snapshot (instantâneo dos dados) não for nulo, processa os documentos.
                    if (snapshot != null) {
                        // Mapeia os documentos do Firestore para uma lista de objetos Task.
                        _tasks.value = snapshot.documents.map { doc ->
                            Task(
                                id = doc.id, // ID do documento da tarefa.
                                title = doc.getString("title") ?: "", // Título da tarefa.
                                done = doc.getBoolean("done") ?: false // Status da tarefa.
                            )
                        }
                    }
                }
        }

        /**
         * Adiciona uma nova tarefa para o usuário logado no Firestore.
         * @param title O título da nova tarefa.
         */
        fun addTask(title: String) {
            val uid = auth.currentUser?.uid ?: return
            // Cria um mapa (HashMap) com os dados da nova tarefa.
            val task = hashMapOf(
                "title" to title,
                "done" to false, // Novas tarefas são marcadas como não concluídas por padrão.
                "createdAt" to com.google.firebase.Timestamp.now() // Adiciona um timestamp de criação.
            )
            // Adiciona a nova tarefa à subcoleção 'tasks' do usuário.
            db.collection("users").document(uid).collection("tasks").add(task)
        }

        /**
         * Alterna o estado 'done' (concluído/não concluído) de uma tarefa existente.
         * @param task O objeto Task a ser atualizado.
         */
        fun toggleTaskDone(task: Task) {
            val uid = auth.currentUser?.uid ?: return
            db.collection("users").document(uid).collection("tasks")
                .document(task.id) // Acessa o documento específico da tarefa pelo seu ID.
                .update("done", !task.done) // Atualiza o campo 'done' para o valor oposto.
        }

        /**
         * Deleta uma tarefa do Firestore.
         * @param task O objeto Task a ser deletado.
         */
        fun deleteTask(task: Task) {
            val uid = auth.currentUser?.uid ?: return
            db.collection("users").document(uid).collection("tasks")
                .document(task.id) // Acessa o documento específico da tarefa.
                .delete() // Deleta o documento.
        }
    }

    /**
     * Verifica o estado de autenticação atual do Firebase e atualiza
     * _authState e _userEmail de acordo.
     */
    private fun refreshUser() {
        val user = auth.currentUser // Obtém o usuário atualmente logado.
        if (user == null) {
            // Se não houver usuário, define o estado como Não Autenticado.
            _authState.value = AuthState.Unauthenticated
            _userEmail.value = "" // Limpa o email do usuário.
        } else {
            // Se houver um usuário, define o estado como Autenticado.
            _authState.value = AuthState.Authenticated
            _userEmail.value = user.email // Define o email do usuário.
        }
    }

    /**
     * Tenta realizar o login de um usuário com email e senha.
     * @param email O email do usuário.
     * @param password A senha do usuário.
     */
    fun login(email: String, password: String) {
        // Validação básica para email e senha.
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email ou senha não podem ser vazios")
            return
        }
        _authState.value = AuthState.Loading // Define o estado como Carregando.
        // Tenta fazer login com o Firebase Auth.
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task -> // Adiciona um listener para quando a operação for concluída.
                if (task.isSuccessful) {
                    refreshUser() // Se o login for bem-sucedido, atualiza o estado do usuário.
                } else {
                    // Se houver erro, define o estado como Erro com a mensagem da exceção.
                    _authState.value = AuthState.Error(task.exception?.message ?: "Erro ao logar")
                }
            }
    }

    /**
     * Tenta criar uma nova conta de usuário com email e senha.
     * @param email O email para a nova conta.
     * @param password A senha para a nova conta.
     */
    fun signup(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email ou senha não podem ser vazios")
            return
        }
        _authState.value = AuthState.Loading
        // Tenta criar um novo usuário com o Firebase Auth.
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    refreshUser() // Se o cadastro for bem-sucedido, atualiza o estado do usuário.
                } else {
                    // Se houver erro, define o estado como Erro.
                    _authState.value =
                        AuthState.Error(task.exception?.message ?: "Erro ao criar usuário")
                }
            }
    }

    /**
     * Realiza o logout do usuário atualmente autenticado.
     */
    fun signout() {
        auth.signOut() // Desconecta o usuário do Firebase Auth.
        _authState.value = AuthState.Unauthenticated // Define o estado como Não Autenticado.
        _userEmail.value = "" // Limpa o email do usuário.
    }


    /**
     * Sealed class para representar os diferentes estados possíveis da autenticação.
     * Usar uma sealed class é bom para representar um conjunto finito de estados.
     */
    sealed class AuthState {
        object Authenticated : AuthState()    // Estado: Usuário está autenticado.
        object Unauthenticated : AuthState()  // Estado: Usuário não está autenticado.
        object Loading : AuthState()          // Estado: Operação de autenticação está em progresso.
        data class Error(val message: String) : AuthState() // Estado: Ocorreu um erro durante a autenticação.
    }
}
