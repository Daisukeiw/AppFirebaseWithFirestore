// Define o pacote ao qual esta classe ViewModel pertence.
package com.example.appfirestore.ToDoTasks

// Importações de classes necessárias.
import android.util.Log // Para registrar mensagens de log (debug, erro, etc.).
import androidx.lifecycle.LiveData // Classe para manter dados observáveis que respeitam o ciclo de vida.
import androidx.lifecycle.MutableLiveData // LiveData que pode ter seu valor modificado.
import androidx.lifecycle.ViewModel // Classe base para ViewModels, projetada para armazenar e gerenciar dados relacionados à UI de forma consciente ao ciclo de vida.
import com.google.firebase.auth.FirebaseAuth // Para obter informações sobre o usuário autenticado, como o UID.
import com.google.firebase.firestore.FirebaseFirestore // Para interagir com o banco de dados Cloud Firestore.

/**
 * TaskViewModel é responsável por gerenciar os dados e a lógica de negócios
 * relacionados às tarefas (Tasks) de um usuário. Ele interage com o Firebase Firestore
 * para buscar, adicionar, atualizar e excluir tarefas.
 */
class TaskViewModel : ViewModel() {
    // Obtém uma instância do Cloud Firestore.
    // Esta é a principal forma de interagir com o banco de dados.
    private val db = FirebaseFirestore.getInstance()
    // Obtém uma instância do FirebaseAuth.
    // Usado aqui principalmente para obter o ID do usuário atualmente logado (UID).
    private val auth = FirebaseAuth.getInstance()

    // MutableLiveData privado que armazena a lista de tarefas (Tasks).
    // Apenas este ViewModel pode modificar diretamente o valor de _tasks.
    private val _tasks = MutableLiveData<List<Task>>()
    // LiveData público e imutável exposto para a UI (Activity/Fragment/Composable).
    // A UI pode observar 'tasks' para receber atualizações quando a lista de tarefas mudar.
    val tasks: LiveData<List<Task>> = _tasks

    /**
     * Carrega as tarefas do usuário atualmente autenticado do Firestore.
     * Esta função configura um listener em tempo real (addSnapshotListener) que
     * atualiza automaticamente a lista de tarefas (_tasks.value) sempre que
     * houver uma alteração nos dados do Firestore.
     */
    fun loadTasks() {
        // Obtém o UID (ID único) do usuário atualmente logado.
        // Se não houver usuário logado (currentUser é nulo), a função retorna e nada acontece.
        val uid = auth.currentUser?.uid ?: return

        // Constrói a referência para a coleção de 'tasks' específica deste usuário.
        // A estrutura de dados no Firestore é: /users/{userId}/tasks/{taskId}
        db.collection("users").document(uid).collection("tasks")
            // Adiciona um listener para mudanças em tempo real nesta coleção.
            // O lambda fornecido será chamado inicialmente com os dados atuais e, depois,
            // sempre que os dados na coleção mudarem.
            .addSnapshotListener { snapshot, e ->
                // Verifica se ocorreu algum erro ao buscar os dados.
                if (e != null) {
                    // Idealmente, um erro deveria ser tratado de forma mais robusta,
                    // talvez expondo um estado de erro para a UI.
                    // Aqui, ele simplesmente retorna, interrompendo o processamento do snapshot.
                    return@addSnapshotListener
                }

                // Verifica se o snapshot (o conjunto de documentos retornados) não é nulo.
                if (snapshot != null) {
                    // Mapeia cada documento do snapshot para um objeto Task.
                    _tasks.value = snapshot.documents.map { doc ->
                        Task(
                            id = doc.id, // O ID do documento no Firestore é usado como ID da tarefa.
                            title = doc.getString("title") ?: "", // Obtém o campo 'title'. Se for nulo, usa uma string vazia.
                            done = doc.getBoolean("done") ?: false // Obtém o campo 'done'. Se for nulo, considera falso.
                        )
                    }
                }
            }
    }

    /**
     * Adiciona uma nova tarefa para o usuário atualmente autenticado no Firestore.
     * @param title O título da nova tarefa a ser adicionada.
     */
    fun addTask(title: String) {
        // Obtém o UID do usuário.
        val uid = auth.currentUser?.uid
        // Se não houver usuário logado, registra um erro e retorna.
        if (uid == null) {
            Log.e("TaskViewModel", "Usuário não logado")
            return
        }

        // Cria um objeto (HashMap) contendo os dados da nova tarefa.
        val task = hashMapOf(
            "title" to title, // Título da tarefa.
            "done" to false   // Novas tarefas são, por padrão, marcadas como não concluídas.
            // Poderia-se adicionar outros campos, como um timestamp de criação.
        )

        // Adiciona o novo objeto 'task' à coleção de tarefas do usuário no Firestore.
        db.collection("users").document(uid).collection("tasks")
            .add(task) // O método 'add' gera automaticamente um ID para o novo documento.
            .addOnSuccessListener { docRef ->
                // Callback chamado se a tarefa for adicionada com sucesso.
                // Registra o ID do documento da nova tarefa.
                Log.d("TaskViewModel", "Tarefa adicionada: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                // Callback chamado se ocorrer um erro ao adicionar a tarefa.
                // Registra o erro.
                Log.e("TaskViewModel", "Erro ao adicionar tarefa", e)
            }
    }

    /**
     * Alterna o estado de 'done' (concluída/não concluída) de uma tarefa existente.
     * @param task O objeto Task cujo estado 'done' será alternado.
     */
    fun toggleTaskDone(task: Task) {
        // Obtém o UID do usuário. Retorna se não houver usuário logado.
        val uid = auth.currentUser?.uid ?: return

        // Acessa o documento específico da tarefa no Firestore usando seu ID.
        db.collection("users").document(uid).collection("tasks")
            .document(task.id) // task.id deve corresponder ao ID do documento no Firestore.
            .update("done", !task.done) // Atualiza o campo 'done' para o valor booleano oposto ao atual.
        // Poderia-se adicionar .addOnSuccessListener e .addOnFailureListener aqui também para tratar o resultado.
    }

    /**
     * Deleta uma tarefa específica do Firestore.
     * @param task O objeto Task a ser deletado.
     */
    fun deleteTask(task: Task) {
        // Obtém o UID do usuário. Retorna se não houver usuário logado.
        val uid = auth.currentUser?.uid ?: return

        // Acessa e deleta o documento específico da tarefa no Firestore.
        db.collection("users").document(uid).collection("tasks")
            .document(task.id) // task.id identifica o documento a ser deletado.
            .delete() // Executa a operação de exclusão.
        // Poderia-se adicionar .addOnSuccessListener e .addOnFailureListener aqui também.
    }
}
