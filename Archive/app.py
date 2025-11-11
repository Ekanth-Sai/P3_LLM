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

@app.route('/process-document', methods=['POST'])
def process_document():
    data = request.get_json()
    file_path = data.get('file_path')
    department = data.get('department', 'General')
    sensitivity = data.get('sensitivity', 'Internal')
    project_name = data.get('project', 'General')
    allowed_roles = data.get('allowed_roles', '').split(',') if data.get('allowed_roles') else []

    if not file_path:
        return jsonify({'error': 'file_path is required'}), 400

    if 'temp_uploads' in file_path:
        relative_path = 'temp_uploads' + file_path.split('temp_uploads', 1)[1]
        app_dir = os.path.dirname(os.path.abspath(__file__))
        file_path = os.path.join(app_dir, '..', relative_path)
        file_path = os.path.abspath(file_path)
    
    try:
        system.process_and_add_document(
            file_path, 
            department, 
            sensitivity, 
            project_name,
            allowed_roles
        )
        return jsonify({
            'message': f"Document added to {department}/{project_name}",
            'status': 'success'
        })
    except Exception as e:
        print(f"Process error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/query', methods=['POST'])
def query():
    data = request.get_json()
    username = data.get('username')
    query_text = data.get('query')
    filters = data.get('filters', {})

    if not username or not query_text:
        return jsonify({'error': 'username and query are required'}), 400
    
    try:
        response = system.query_system(username, query_text, filters)

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
            'filters': filters,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })

        with open(CONVERSATION_HISTORY_LOG, "w") as f:
            json.dump(history, f, indent=4)

        return jsonify({'response': response, 'status': 'success'})
    except Exception as e:
        print(f"Query error: {e}")
        return jsonify({'error': str(e)}), 500

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

@app.route("/delete-document", methods=["POST"])
def delete_document():
    data = request.get_json()
    filename = data.get("filename")
    department = data.get("department")  # Optional

    if not filename:
        return jsonify({"error": "filename is required"}), 400 
    
    try:
        deleted_count = system.vector_db_manager.delete_document_by_filename(
            filename, 
            department
        )
        
        if deleted_count > 0:
            if os.path.exists(PROCESSED_FILES_LOG):
                with open(PROCESSED_FILES_LOG, "r") as f:
                    lines = f.readlines()
                
                with open(PROCESSED_FILES_LOG, "w") as f:
                    for line in lines:
                        if filename not in line:
                            f.write(line)
            
            return jsonify({
                "message": f"Deleted {filename} ({deleted_count} chunks)",
                "status": "success"
            })
        else:
            return jsonify({
                "message": f"{filename} not found",
                "status": "not_found"
            }), 404
            
    except Exception as e:
        print(f"Delete error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/processed-documents', methods=['GET'])
def get_processed_documents():
    if not os.path.exists(PROCESSED_FILES_LOG):
        return jsonify([])
    with open(PROCESSED_FILES_LOG, "r") as f:
        files = [line.strip() for line in f.readlines()]
    return jsonify(files)

if __name__ == '__main__':
    print("Starting Flask server with Hybrid RBAC...") #Delete after test
    app.run(host='0.0.0.0', port=5001, debug=True)