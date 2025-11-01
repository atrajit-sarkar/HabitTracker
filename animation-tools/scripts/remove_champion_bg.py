"""
Remove background from champion.png ONLY using AI (rembg)
"""
import sys
import os
from pathlib import Path
from PIL import Image

try:
    from rembg import remove
    REMBG_AVAILABLE = True
except ImportError:
    REMBG_AVAILABLE = False
    print("⚠️  rembg not available. Install with: pip install rembg")

def remove_background_ai(input_path, output_path):
    """
    Remove background using AI model (rembg) with best quality settings
    
    Args:
        input_path: Path to input PNG
        output_path: Path to output PNG
    """
    if not REMBG_AVAILABLE:
        print(f"❌ Cannot process {input_path} - rembg not installed")
        print("Install with: pip install rembg")
        return False
    
    print(f"🤖 AI Processing champion image: {input_path}")
    
    try:
        # Open image
        with open(input_path, 'rb') as input_file:
            input_data = input_file.read()
        
        # Remove background using AI with basic settings to preserve all foreground
        # No alpha matting - just basic background removal
        output_data = remove(input_data)
        
        # Save result
        with open(output_path, 'wb') as output_file:
            output_file.write(output_data)
        
        print(f"✅ Background removed successfully!")
        print(f"✅ Saved to: {output_path}")
        return True
    except Exception as e:
        print(f"❌ Error processing {input_path}: {e}")
        return False

def process_champion_only():
    """Process ONLY the champion.png file"""
    script_dir = Path(__file__).parent.parent.parent  # Go up to project root
    input_dir = script_dir / 'widgets-assets'
    output_dir = script_dir / 'app' / 'src' / 'main' / 'res' / 'drawable'
    
    # Create output directory if it doesn't exist
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Process ONLY champion.png
    input_path = input_dir / 'champion.png'
    output_path = output_dir / 'widget_champion.png'
    
    if not input_path.exists():
        print(f"❌ File not found: {input_path}")
        return False
    
    print(f"📁 Input:  {input_path}")
    print(f"📁 Output: {output_path}")
    print("")
    
    success = remove_background_ai(str(input_path), str(output_path))
    
    if success:
        print(f"\n✨ Champion image processed successfully!")
        print(f"🎉 Ready to use in widget as: R.drawable.widget_champion")
    else:
        print(f"\n❌ Failed to process champion image")
    
    return success

if __name__ == '__main__':
    print("🏆 Removing background from CHAMPION image ONLY using AI...\n")
    process_champion_only()
    print("\n✅ Done!")
