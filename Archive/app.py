from flask import Flask, request, jsonify
from flask_cors import CORS
from main import DocQASystem
import os
import json
from datetime import datetime, timezone

app = Flask(__name__)
CORS(app)
system = DocQASystem()

PROCESSED_FILES_LOG = os.path.join("chroma_db", "processed_files.log")
CONVERSATION_HISTORY_LOG = os.path.join("chroma_db", "conversation_history.json")

@app.route('/flush-db', methods=['POST'])
def flush_db():
    system.vector_db_manager.reset_collection()
    # Also clear the processed files log
    if os.path.exists(PROCESSED_FILES_LOG):
        os.remove(PROCESSED_FILES_LOG)
    return jsonify({'message': 'ChromaDB collection and processed files log have been reset.'})

@app.route('/users', methods=['GET'])
def get_users():
    users = [{ "email": email, "role": roles[0]} for email, roles in system.access_manager.USERS_ROLES.items()]
    return jsonify(users)

@app.route('/is-admin', methods=['GET'])
def is_admin():
    username = request.args.get('username')
    if not username:
        return jsonify({'error': 'username is required'}), 400

    user_roles = system.access_manager.get_user_roles(username)
    return jsonify({'is_admin': 'admin' in user_roles})

@app.route('/process-document', methods=['POST'])
def process_document():
    data = request.get_json()
    file_path = data.get('file_path')
    department = data.get('department', 'General')
    sensitivity = data.get('sensitivity', 'Public')

    if not file_path:
        return jsonify({'error': 'file_path is required'}), 400
    
    try:
        system.process_and_add_document(file_path, department, sensitivity)
        return jsonify({'message': f"Successfully processed {file_path}"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/processed-documents', methods=['GET'])
def get_processed_documents():
    if not os.path.exists(PROCESSED_FILES_LOG):
        return jsonify([])
    with open(PROCESSED_FILES_LOG, "r") as f:
        files = [line.strip() for line in f.readlines()]
    return jsonify(files)

@app.route('/history', methods=['GET'])
def get_history():
    username = request.args.get('username')
    if not username:
        return jsonify({'error': 'username is required'}), 400

    if not os.path.exists(CONVERSATION_HISTORY_LOG):
        return jsonify([])

    with open(CONVERSATION_HISTORY_LOG, "r") as f:
        try:
            history = json.load(f)
        except json.JSONDecodeError:
            history = []

    user_history = [conv for conv in history if conv.get('username') == username]
    return jsonify(user_history)

@app.route('/query', methods=['POST'])
def query():
    data = request.get_json()
    username = data.get('username')
    query_text = data.get('query')

    if not username or not query_text:
        return jsonify({'error': 'username and query are required'}), 400
    
    try:
        response = system.query_system(username, query_text)

        # Log the conversation
        if not os.path.exists(CONVERSATION_HISTORY_LOG):
            history = []
        else:
            with open(CONVERSATION_HISTORY_LOG, "r") as f:
                try:
                    history = json.load(f)
                except json.JSONDecodeError:
                    history = []
        
        history.append({
            'username': username,
            'query': query_text,
            'response': response,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })

        with open(CONVERSATION_HISTORY_LOG, "w") as f:
            json.dump(history, f, indent=4)

        return jsonify({'response': response})
    except Exception as e:
        return jsonify({'error': str(e)}), 500
    
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)