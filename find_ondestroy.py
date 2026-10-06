import os
import re

for root, dirs, files in os.walk(r'd:\Documents\KapwaDVOv2\app\src\main\java'):
    for file in files:
        if file.endswith('.kt'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Find onDestroyView body
            match = re.search(r'fun onDestroyView\(\)\s*\{([^}]*)\}', content)
            if match:
                body = match.group(1)
                if 'launch' in body:
                    print(f"Found in {filepath}: {body}")
