`jackson-2.13.5-payloads.json` records `SerializationCases.values()` serialized by
Jackson databind/core/annotations 2.13.5 against SDK models from commit `fcbd188`.
The baseline was generated separately from the replacement serializers.
The five array cases added later were generated with Jackson 2.13.5 from the raw
array values under the same `userjourney` wire key.

`HCaptchaSerializationTest` compares parsed JSON strictly: object key order and
escape spelling may differ; field names, values, nulls, array order, and omitted
fields must match. Jackson is not needed to run these tests.
