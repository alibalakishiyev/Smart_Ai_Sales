
# MobileBERT TFLite Models

* Source: [Google Research - Mobile BERT](https://github.com/google-research/google-research/tree/master/mobilebert)
* Task: Q&A
* Model: 30k vocab size, sequence length of 384
* Dataset: Squad 1.1
* Inputs: `input_ids` (int32), `input_mask` (int32), `segment_ids` (int32)
* Outputs: `start_logits`, `end_logits`

## Float (96MB)

Accuracy: 90 F1

This version uses the same source as that of the [already published .tflite
model](https://tfhub.dev/tensorflow/lite-model/mobilebert/1/default/1). The
primary difference is that this version was converted using the new TFLite
conversion backend in the TF 2.2 release.

## Quantized (26MB)

Accuracy: 88 F1

Created using a combination of quant-aware training and post-training quant
techniques. Source and conversion scripts will be made available soon.

## License
[Apache 2](https://opensource.org/licenses/Apache-2.0)
