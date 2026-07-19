import json
import sys

def generate_voxel_shape(json_path):
    try:
        with open(json_path, 'r') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"エラー: ファイル '{json_path}' が見つかりませんでした。")
        return

    elements = data.get('elements')
    if not elements:
        print("エラー: 'elements' が見つかりませんでした。親モデルを継承している場合は解決できません。")
        return

    boxes = []
    for el in elements:
        # 座標を取得
        x1, y1, z1 = el['from']
        x2, y2, z2 = el['to']
        
        # Block.boxは double型を受け取るので小数点をつける
        box_str = f"Block.box({x1}, {y1}, {z1}, {x2}, {y2}, {z2})"
        boxes.append(box_str)

    # Javaコードの生成
    print("// " + json_path + " から生成された形状データ")
    if len(boxes) == 1:
        print(f"public static final VoxelShape SHAPE = {boxes[0]};")
    else:
        print("public static final VoxelShape SHAPE = Shapes.or(")
        print("    " + ",\n    ".join(boxes))
        print(");")

if __name__ == "__main__":
    # 使い方: python model_to_shape.py <jsonファイルへのパス>
    if len(sys.argv) < 2:
        # 引数がない場合はカレントディレクトリの seat_block.json を探す（例）
        target = "seat_block.json" 
    else:
        target = sys.argv[1]
    
    generate_voxel_shape(target)