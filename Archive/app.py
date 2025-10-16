from flask import Flask, request, jsonify
from flask_cors import CORS
from main import DocQASystem

app = Flask(__name__)
CORS(app)
system = DocQASystem()

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

@app.route('/query', methods=['POST'])
def query():
    data = request.get_json()
    username = data.get('username')
    query_text = data.get('query')

    if not username or not query_text:
        return jsonify({'error': 'username and query are required'}), 400
    
    try:
        response = system.query_system(username, query_text)
        return jsonify({'response': response})
    except Exception as e:
        return jsonify({'error': str(e)}), 500
    
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)